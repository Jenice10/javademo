node {
    def CMD_STOP_NGINX = 'sudo systemctl stop openresty.service'
    def CMD_START_NGINX = 'sudo systemctl start openresty.service'
    def CMD_RESTART_APP = 'sudo systemctl restart myapp.service'
    def CHECK_BACKEND_SLEEP_SECS = 8
    def CHECK_NGINX_SLEEP_SECS = 1
    def CHECK_MAX_WAIT_SECS = 30
    def CONSUL_SLEEP_TIME = 35
//     def SERVERS_MAP = '''
// { "servers" : [
//     {"name": "staging", "profile": "staging", "ips": ["10.0.0.1"]},
//     {"name": "prod1", "profile": "prod", "ips": ["10.0.0.2", "10.0.0.3"]},
//     {"name": "prod2", "profile": "prod", "ips": ["10.0.0.4", "10.0.0.5"]}
// ]
// }
// '''
    // set true when use for production
    def debug = false

    // echo 'debug = ' + debug.toString()
    // echo 'CMD_RESTART_APP= '+ CMD_RESTART_APP

    // properties([
    //     disableConcurrentBuilds(),
    //     parameters([
    //         gitParameter(branch: '', branchFilter: '.*', defaultValue: '', description: '构建使用的分支号', name: 'tag', quickFilterEnabled: false, selectedValue: 'NONE', sortMode: 'NONE', tagFilter: '*', type: 'PT_BRANCH_TAG'),
    //         //  choice(choices: ['dev', 'prod'], description: '在线环境prod，其他环境dev', name: 'profile'),
    //         text(defaultValue: "${SERVERS_MAP}", description: '发布服务器组', name: 'serversMap', trim: true),
    //         booleanParam(defaultValue: true, description: '是否需要确认', name: 'confirming'),
    //         string(defaultValue: ':80/actuator/health', description: 'nginx health endpoint', name: 'nginxHealthEndpoint', trim: true),
    //         string(defaultValue: ':8080/actuator/health', description: 'backend endpoint', name: 'backendHealthEndpoint', trim: true),
    //         string(defaultValue: 'odin', description: '远端服务器操作用户', name: 'remoteUser', trim: true),
    //         booleanParam(defaultValue: false, description: '是否重启nginx', name: 'nginx'),
    //         booleanParam(defaultValue: false, description: '仅重启服务器应用', name: 'rebootOnly'),
    //         booleanParam(defaultValue: false, description: '发布过程中是否维护consul', name: 'consul'),
    //         string(defaultValue: 'service', description: '注册在consul的服务名', name: 'consulServiceName', trim: true)
    //         ])
    //     ]
    // )

    def servers = readJSON text: serversMap
    servers['servers'].find {
        server -> if (!server.containsKey("name") || !server.containsKey("profile") || !server.containsKey("ips")) {
            error "illegal parameter serversMap, must contains both name|profile|ips attribute."
        }
        for (ip in server['ips']) {
            if (!isValidIp(ip)) {
                error "illegal ip found in serversMap: " + ip
            }
        }
    }
    // echo servers['servers'].getClass().toString()
    // echo servers['servers'].size().toString()
    if (servers['servers'].size() < 1) {
        error "no servers to deploy"
    }

    // servers['servers'].each {
    //     server -> echo server.getClass().toString()
    // }
    stage('checkout') {
        checkout scm
    }

    stage('assemble') {
        if (debug) {
            echo './gradlew assemble'
        } else {
            sh 'chmod +x ./gradlew && ./gradlew assemble'
        }
    }

    servers['servers'].find {
        server -> 
            stage(server['name']) {
                if (confirming == 'true') {
                    input 'make sure to publish ' + server['name']
                }
                for (ip in server['ips']) {
                    if (!deploy(ip, remoteUser, server['profile'], backendHealthEndpoint, nginxHealthEndpoint, nginx, consul, CMD_STOP_NGINX, 
                        CMD_RESTART_APP, CMD_START_NGINX, consulServiceName, CONSUL_SLEEP_TIME, CHECK_MAX_WAIT_SECS, CHECK_NGINX_SLEEP_SECS, 
                        CHECK_BACKEND_SLEEP_SECS, debug)) {
                        error "deploy ["+ip+"] failed" 
                    }
                }
                echo server['name'] + " deployed"
            }
    }
}

def deploy(ip, remoteUser, profile, backendHealthEndpoint, nginxHealthEndpoint, nginx, consul, CMD_STOP_NGINX, CMD_RESTART_APP, CMD_START_NGINX, 
    consulServiceName, CONSUL_SLEEP_TIME, CHECK_MAX_WAIT_SECS, CHECK_NGINX_SLEEP_SECS, CHECK_BACKEND_SLEEP_SECS, debug) {
    def sshLoc = remoteUser + '@' + ip
    // stop nginx to stop traffic
    if (nginx == 'true') {
        if (debug) {
            echo "ssh ${sshLoc} \"${CMD_STOP_NGINX}\""
        } else {
            sh "ssh ${sshLoc} \"${CMD_STOP_NGINX}\""
        }
    }
    // disable consul
    if (consul == 'true') {
        if (debug) {
            echo "ssh ${sshLoc} \"curl --request PUT http://127.0.0.1:8500/v1/agent/service/maintenance/${consulServiceName}?enable=true&reason=deployservice\""
        } else {
            sh "ssh ${sshLoc} \"curl --request PUT http://127.0.0.1:8500/v1/agent/service/maintenance/${consulServiceName}?enable=true&reason=deployservice\""
	        sleep(CONSUL_SLEEP_TIME) 
        }
    }
    // if rebootOnly, not upgrade files to server
    if (rebootOnly == 'false') {
        if (debug) {
            echo "scp build/libs/app.jar ${sshLoc}:/search/odin/app/app.jar"
            echo "scp envfiles/${profile}.conf ${sshLoc}:/search/odin/app/app.conf"
        } else {
            sh "scp build/libs/app.jar ${sshLoc}:/search/odin/app/app.jar"
            sh "scp envfiles/${profile}.conf ${sshLoc}:/search/odin/app/app.conf"
        }
    }
    // restart app
    if (debug) {
        echo "ssh ${sshLoc} \"${CMD_RESTART_APP}\""
    } else {
        sh "ssh ${sshLoc} \"${CMD_RESTART_APP}\""
    }
    if (!debug && !check(ip + backendHealthEndpoint, CHECK_MAX_WAIT_SECS, CHECK_BACKEND_SLEEP_SECS)) {
        return false
    }
    // enable nginx
    if (nginx == 'true') {
        if (debug) {
            echo "ssh ${sshLoc} \"${CMD_START_NGINX}\""
        } else {
            sh "ssh ${sshLoc} \"${CMD_START_NGINX}\""
            return check(ip + nginxHealthEndpoint, CHECK_MAX_WAIT_SECS, CHECK_NGINX_SLEEP_SECS)
        }
    }
    // enable consul
    if (consul == 'true') {
        if (debug) {
            echo "ssh ${sshLoc} \"curl --request PUT http://127.0.0.1:8500/v1/agent/service/maintenance/${consulServiceName}?enable=false&reason=deployservice\""
        } else {
            sh "ssh ${sshLoc} \"curl --request PUT http://127.0.0.1:8500/v1/agent/service/maintenance/${consulServiceName}?enable=false&reason=deployservice\""
        }
    }
    return true
}

def check(url, max, initial_sleep_secs) {
    // return true when url is empty 
    if (!url?.trim()) {
        echo "url is empty, return true😀"
        return true
    }
    if (!url.startsWith("http")) {
        url = 'http://' + url
    }
    def cmd = $/eval "curl -s ${url} | sed 's/ //g' | grep '\"status\":\"UP\"' | wc -l"/$
    echo "${cmd}"
    def rc = "0"
    sleep(initial_sleep_secs); // seconds
    try {
        rc = sh(
                script: "${cmd}",
                returnStdout: true
        ).trim()
        echo rc
    } catch (Exception e) {
    }
    def i = 0;
    while (rc.equals("0") && i < max) {
        sleep(1) // seconds
        try {
            rc = sh(
                    script: "${cmd}",
                    returnStdout: true
            ).trim()
            echo rc
        } catch (Exception e) {
        }
        i++
    }
    echo rc
    return rc.equals("0") ? false : true
}

// 判断是否是合法ipv4
def isValidIp(ip) {
    return true
    // try {
    //     String[] parts = ip.split("\\.");
    //     if (parts.length != 4) return false;
    //     for (int i = 0; i < 4; ++i) {
    //         int p = Integer.parseInt(parts[i]);
    //         if (p > 255 || p < 0) return false;
    //     }
    //     return true;
    // } catch (Exception e) {
    //     e.printStackTrace();
    //     return false;
    // }
}