import {ConfigMITM} from './ConfigMITM'
import {XmppMITM} from './XmppMITM'
import {getRiotClientPath, isRiotClientRunning} from './riotClientUtils'
import {exec} from 'node:child_process'

(async () => {
    const httpPort = 35479
    const xmppPort = 35478
    const host = '127.0.0.1'

    if (await isRiotClientRunning()) {
        console.log(JSON.stringify({
            type: 'error',
            code: 409,
            reason: 'Riot client is running, please close it before running this tool.'
        }) + '\n')
        process.exit(1)
    }
    const configMitm = new ConfigMITM(httpPort, host, xmppPort)
    await configMitm.start()
    const xmppMitm = new XmppMITM(xmppPort, host, configMitm)
    await xmppMitm.start()
    const riotClientPath = await getRiotClientPath()
    exec(`"${riotClientPath}" --client-config-url="http://${host}:${httpPort}" --launch-product=valorant --launch-patchline=live`)
})()