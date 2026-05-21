import { Injectable } from '@angular/core';

declare var window: any;

@Injectable({
  providedIn: 'root',
})
export class Serial {

  listeners: ((data: string) => void)[] = [];

  constructor() {
    this.initializeSerialListener();
  }

   // =============================
  // RX LISTENER FROM ANDROID
  // =============================
  private initializeSerialListener() {

    window.addEventListener('serialData', (event: any) => {

      try {

        const payload = JSON.parse(event.detail);

        this.listeners.forEach(cb => cb(payload));

      } catch (e) {
        console.error('Serial Parse Error', e);
      }
    });
  }

  // =============================
  // SUBSCRIBE TO DATA
  // =============================
  onData(callback: (data: any) => void) {
    this.listeners.push(callback);
  }

  // =============================
  // SEND SERIAL DATA
  // =============================
  async send(message: string) {

    try {

      if ((window as any).SerialAndroid) {

        (window as any).SerialAndroid.send(message);

      } else {

        console.error('SerialAndroid bridge not found');
      }

    } catch (e) {

      console.error('Send Error', e);
    }
  }

  // =============================
  // CONNECT
  // =============================
  async connect(baudrate: number = 115200) {

    try {

      if ((window as any).SerialAndroid) {

        (window as any).SerialAndroid.connect(baudrate);

      }

    } catch (e) {

      console.error('Connect Error', e);
    }
  }

  // =============================
  // DISCONNECT
  // =============================
  async disconnect() {

    try {

      if ((window as any).SerialAndroid) {

        (window as any).SerialAndroid.disconnect();

      }

    } catch (e) {

      console.error('Disconnect Error', e);
    }
  }

}
