import { Injectable } from '@angular/core';

declare var window: any;

// =============================
// TYPES (terminal-grade structure)
// =============================
export interface SerialPacket {
  data?: string;
  status?: string;
  timestamp?: number;
  type?: 'rx' | 'tx' | 'status';
}

type SerialCallback = (packet: SerialPacket) => void;

@Injectable({
  providedIn: 'root',
})
export class Serial {

  // =============================
  // INTERNAL STATE
  // =============================

  private listeners: Set<SerialCallback> = new Set();

  private isInitialized = false;

  // =============================
  // INIT
  // =============================

  constructor() {
    this.initialize();
  }

  // =============================
  // LISTENER FROM ANDROID
  // =============================

  private initialize() {

    if (this.isInitialized) return;

    this.isInitialized = true;

    // RX DATA
    window.addEventListener('serialData', (event: any) => {

      try {

        const payload = JSON.parse(event.detail);

        this.emit({
          data: payload.data,
          timestamp: payload.timestamp,
          type: 'rx'
        });

      } catch (err) {
        console.error('RX Parse Error', err);
      }
    });

    // STATUS EVENTS (NEW)
    window.addEventListener('serialStatus', (event: any) => {

      try {

        const payload = JSON.parse(event.detail);

        this.emit({
          status: payload.status,
          type: 'status',
          timestamp: Date.now()
        });

      } catch (err) {
        console.error('Status Parse Error', err);
      }
    });
  }

  // =============================
  // EMITTER (SAFE MULTI-SUBSCRIBER)
  // =============================

  private emit(packet: SerialPacket) {

    this.listeners.forEach(cb => {
      try {
        cb(packet);
      } catch (e) {
        console.error('Listener error', e);
      }
    });
  }

  // =============================
  // SUBSCRIBE
  // =============================

  onData(callback: SerialCallback): () => void {

    this.listeners.add(callback);

    // return unsubscribe function (IMPORTANT FIX)
    return () => {
      this.listeners.delete(callback);
    };
  }

  // =============================
  // SEND
  // =============================

  send(message: string) {

    try {

      if (window?.SerialAndroid?.send) {
        window.SerialAndroid.send(message);
      } else {
        console.error('SerialAndroid not available');
      }

    } catch (e) {
      console.error('Send Error', e);
    }
  }

  // =============================
  // CONNECT
  // =============================

  connect(baudrate: number = 115200) {

    try {

      if (window?.SerialAndroid?.connect) {
        window.SerialAndroid.connect(baudrate);
      }

    } catch (e) {
      console.error('Connect Error', e);
    }
  }

  // =============================
  // DISCONNECT
  // =============================

  disconnect() {

    try {

      if (window?.SerialAndroid?.disconnect) {
        window.SerialAndroid.disconnect();
      }

    } catch (e) {
      console.error('Disconnect Error', e);
    }
  }

  // =============================
  // OPTIONAL UTILITY
  // =============================

  clearAllListeners() {
    this.listeners.clear();
  }

}
