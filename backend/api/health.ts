import type { IncomingMessage, ServerResponse } from 'node:http';

type Request = IncomingMessage;
type Response = ServerResponse & { status(code: number): Response; json(value: unknown): Response };

export default function handler(_request: Request, response: Response) {
  response.setHeader('Cache-Control', 'no-store');
  return response.status(200).json({ ok: true, service: 'medlens-api' });
}
