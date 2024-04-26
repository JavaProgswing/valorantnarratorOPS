export declare class ConfigMITM {
    affinityMappings: {
        localHost: string;
        riotHost: string;
        riotPort: number;
    }[];
    private readonly _port;
    private readonly _host;
    private readonly _xmppPort;
    private _server;
    private _affinityMappingID;
    constructor(port: number, host: string, xmppPort: number);
    start(): Promise<void>;
    stop(): Promise<void>;
}
