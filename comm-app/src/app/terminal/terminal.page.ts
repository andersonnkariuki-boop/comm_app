import { Component, OnInit, OnDestroy } from '@angular/core';
import { Serial, SerialPacket } from '../services/serial';
import { Router } from '@angular/router';

@Component({
  selector: 'app-terminal',
  templateUrl: './terminal.page.html',
  styleUrls: ['./terminal.page.scss'],
  standalone: false,
})
export class TerminalPage implements OnInit, OnDestroy {

  logs: string[] = [];
  message = '';

  hex = false;

  lineEnding: 'none' | 'lf' | 'cr' | 'crlf' = 'none';

  private unsubscribe?: () => void;

  constructor(
    private serial: Serial,
    private router: Router
  ) {}

  // =============================
  // INIT
  // =============================
  ngOnInit() {

    this.unsubscribe = this.serial.onData((packet: SerialPacket) => {

      const time = new Date(packet.timestamp || Date.now())
        .toLocaleTimeString();

      // =============================
      // RX
      // =============================
      if (packet.type === 'rx' && packet.data) {
        this.logs.push(`[${time}] RX: ${packet.data}`);
      }

      // =============================
      // STATUS
      // =============================
      if (packet.type === 'status' && packet.status) {
        this.logs.push(`[${time}] STATUS: ${packet.status}`);
      }

      this.autoScroll();
    });
  }

  // =============================
  // SEND
  // =============================
  send() {

    if (!this.message.trim()) return;

    const time = new Date().toLocaleTimeString();

    this.logs.push(`[${time}] TX: ${this.message}`);

    this.serial.send(this.message);

    this.message = '';

    this.autoScroll();
  }

  // =============================
  // HEX TOGGLE
  // =============================
  toggleHex() {

    this.hex = !this.hex;

    this.serial.setHex(this.hex);
  }

  // =============================
  // LINE ENDING
  // =============================
  updateLineEnding() {
    this.serial.setLineEnding(this.lineEnding);
  }

  // =============================
  // CLEAR TERMINAL
  // =============================
  clear() {
    this.logs = [];
  }

  // =============================
  // AUTO SCROLL (SMOOTH + STABLE)
  // =============================
  autoScroll() {

    requestAnimationFrame(() => {

      const el = document.querySelector('.terminal');

      if (el) {
        el.scrollTop = el.scrollHeight;
      }

    });
  }

  // =============================
  // CLEANUP
  // =============================
  ngOnDestroy() {

    if (this.unsubscribe) {
      this.unsubscribe();
    }
  }
}
