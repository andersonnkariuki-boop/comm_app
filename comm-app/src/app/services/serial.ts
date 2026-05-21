import { Injectable } from '@angular/core';

declare var window: any;

// =============================
// TYPES (industrial terminal-grade)
// =============================
export interface SerialPacket {
  data?: string;
  status?: string;
  timestamp?: number;
  type: 'rx' | 'tx' | 'status';
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

  private initialize() {

    if (this.isInitialized) return;
    this.isInitialized = true;

    // =============================
    // RX DATA FROM ANDROID
    // =============================
    window.addEventListener('serialData', (event: any) => {

      try {

        // 🔥 FIX: Capacitor sends JSON string in event.detail OR event itself
        const raw = event?.detail || event;

        const payload =
          typeof raw === 'string'
            ? JSON.parse(raw)
            : raw;

        this.emit({
          data: payload.data,
          timestamp: payload.timestamp,
          type: 'rx'
        });

      } catch (err) {
        console.error('RX Parse Error', err);
      }
    });

    // =============================
    // STATUS EVENTS
    // =============================
    window.addEventListener('serialStatus', (event: any) => {

      try {

        const raw = event?.detail || event;

        const payload =
          typeof raw === 'string'
            ? JSON.parse(raw)
            : raw;

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
  // EMITTER
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

    return () => {
      this.listeners.delete(callback);
    };
  }

  // =============================
  // SEND DATA
  // =============================

  send(message: string) {

    try {

      window?.SerialAndroid?.send?.(message);

    } catch (e) {
      console.error('Send Error', e);
    }
  }

  // =============================
  // CONNECT
  // =============================

  connect(baudrate: number = 115200) {

    try {

      window?.SerialAndroid?.connect?.(baudrate);

    } catch (e) {
      console.error('Connect Error', e);
    }
  }

  // =============================
  // HEX MODE
  // =============================

  setHex(enabled: boolean) {

    try {

      window?.SerialAndroid?.setHex?.(enabled);

    } catch (e) {
      console.error('Hex Mode Error', e);
    }
  }

  // =============================
  // LINE ENDING CONTROL (NEW)
  // =============================

  setLineEnding(mode: 'none' | 'lf' | 'cr' | 'crlf') {

    try {

      window?.SerialAndroid?.setLineEnding?.(mode);

    } catch (e) {
      console.error('Line Ending Error', e);
    }
  }

  // =============================
  // DISCONNECT
  // =============================

  disconnect() {

    try {

      window?.SerialAndroid?.disconnect?.();

    } catch (e) {
      console.error('Disconnect Error', e);
    }
  }

  // =============================
  // UTIL
  // =============================

  clearAllListeners() {
    this.listeners.clear();
  }
}