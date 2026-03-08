import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAccessToken, API_URL } from '../api/client';

export interface NotificationPayload {
  eventType: string;
  termCode: string;
  sisSectionId: string;
  createdAt: string;
}

export function useNotifications(onNotification: (payload: NotificationPayload) => void) {
  const onNotificationRef = useRef(onNotification);
  onNotificationRef.current = onNotification;
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    const token = getAccessToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws`) as unknown as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (msg) => {
          try {
            const payload: NotificationPayload = JSON.parse(msg.body);
            onNotificationRef.current(payload);
          } catch {
            // ignore
          }
        });
      },
      onStompError: () => {},
    });
    client.activate();
    clientRef.current = client;
  }, []);

  useEffect(() => {
    connect();
    return () => {
      clientRef.current?.deactivate();
      clientRef.current = null;
    };
  }, [connect]);

  return { reconnect: connect };
}
