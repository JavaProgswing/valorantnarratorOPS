import {ConfigMITM} from './ConfigMITM';

export declare class XmppMITM {
    private readonly _port;
    private readonly _host;
    private readonly _configMitm;
    private _socketID;
    constructor(port: number, host: string, configMitm: ConfigMITM);
    start(): Promise<void>;
}
