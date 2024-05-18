import * as tls from 'node:tls'
import {ConfigMITM} from './ConfigMITM'
import * as fs from 'node:fs'
import * as path from 'node:path'

export class XmppMITM {
    private readonly _port: number
    private readonly _host: string
    private readonly _configMitm: ConfigMITM
    private _socketID = 0

    constructor(port: number, host: string, configMitm: ConfigMITM) {
        this._port = port
        this._host = host
        this._configMitm = configMitm
    }
    async start() {
        return new Promise<void>(async (resolve) => {
            let cliHooked = false;
            tls.createServer({
                key: await fs.promises.readFile(path.join(__dirname, 'certs/server.key')),
                cert: await fs.promises.readFile(path.join(__dirname, 'certs/server.cert')),
                rejectUnauthorized: false,
                requestCert: false
            }, socket => {
                const ipv4LocalHost = socket.localAddress?.replace('::ffff:', '')
                const mapping = this._configMitm.affinityMappings.find(mapping => mapping.localHost === ipv4LocalHost)
                if (mapping === undefined) {
                    console.log(JSON.stringify({
                        type: 'error',
                        code: 500,
                        reason: `Unknown host ${socket.localAddress}`
                    }) + '\n')
                    socket.destroy()
                    return
                }

                this._socketID++
                const currentSocketID = this._socketID

                console.log(JSON.stringify({
                    type: 'open-valorant',
                    time: Date.now(),
                    host: mapping.riotHost,
                    port: mapping.riotPort,
                    socketID: currentSocketID
                }) + '\n')

                let preConnectBuffer = Buffer.alloc(0)
                const riotTLS = tls.connect({
                    host: mapping.riotHost,
                    port: mapping.riotPort,
                    rejectUnauthorized: false,
                    requestCert: false
                }, () => {
                    if (preConnectBuffer.length > 0) {
                        riotTLS.write(preConnectBuffer)
                        preConnectBuffer = Buffer.alloc(0)
                    }
                    handleStdinData(riotTLS);
                    console.log(JSON.stringify({
                        type: 'open-riot',
                        time: Date.now(),
                        socketID: currentSocketID
                    }) + '\n')
                })

                const handleStdinData = (riotTLS: tls.TLSSocket) => {
                    if (!cliHooked) {
                        process.stdin.setEncoding('utf8');
                        process.stdin.on('data', (data) => {
                            if (!riotTLS.connecting) {
                                riotTLS.write(data); // Write data to riotTLS if it's connected
                            }
                        });
                        cliHooked = true;
                    }
                };

                riotTLS.on('data', data => {
                    console.log(JSON.stringify({
                        type: 'incoming',
                        time: Date.now(),
                        data: data.toString()
                    }) + '\n')
                    socket.write(data)
                })

                riotTLS.on('close', () => {
                    console.log(JSON.stringify({
                        type: 'close-riot',
                        time: Date.now(),
                        socketID: currentSocketID
                    }) + '\n')
                })

                socket.on('data', data => {
                    console.log(JSON.stringify({
                        type: 'outgoing',
                        time: Date.now(),
                        data: data.toString()
                    }) + '\n')
                    if (riotTLS.connecting) {
                        preConnectBuffer = Buffer.concat([preConnectBuffer, data])
                    } else {
                        riotTLS.write(data)
                    }
                })

                socket.on('close', () => {
                    console.log(JSON.stringify({
                        type: 'close-valorant',
                        time: Date.now(),
                        socketID: currentSocketID
                    }) + '\n')
                })
            }).listen(this._port, () => {
                resolve()
            })
        })
    }
}