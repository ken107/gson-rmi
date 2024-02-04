
export declare class Callback {
  constructor(target: Route, method: string, args?: unknown[]);
  target: Route;
  method: string;
  args: unknown[];
  lastSent: number;
}

export declare class RmiService {
  constructor(sendFunc: (m: Message) => void, logError: Console["error"]);
  register(targetId: string, target: Record<string, Function>): void;
  unregister(targetId: string): void;
  call(dest: Route, method: string, args?: unknown[], callback?: Callback, session?: object): void;
  receive: (m: Message) => void;
}

export declare class Route {
  constructor(...hops: string[]);
  hops: string[];
}

export interface Message {
  src: Route
  dests: Route[]
  contentType: string
  content: JsonRpcRequest|JsonRpcSuccessResponse|JsonRpcErrorResponse
}

export interface JsonRpcRequest {
  jsonrpc: "2.0"
  method: string
  params?: unknown[]
  id?: number
}

export interface JsonRpcSuccessResponse {
  jsonrpc: "2.0"
  result: unknown
  id: number
}

export interface JsonRpcErrorResponse {
  jsonrpc: "2.0"
  error: JsonRpcError
  id: number
}

export interface JsonRpcError {
  code: number
  message: string
  data?: unknown
}
