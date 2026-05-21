import { Component, OnInit } from '@angular/core';
import { Serial } from '../services/serial';

declare var window: any;

@Component({
  selector: 'app-terminal',
  templateUrl: './terminal.page.html',
  styleUrls: ['./terminal.page.scss'],
  standalone: false,
})
export class TerminalPage implements OnInit {
  logs: string[] = [];
  msg = '';
  hex = false;

  constructor(private serial: Serial) { }

  ngOnInit() {
        // =============================
    // RECEIVE SERIAL DATA
    // =============================
    this.serial.onData((payload: any) => {

      const time =
        new Date(payload.timestamp)
          .toLocaleTimeString();

      this.logs.push(
        `[${time}] RX: ${payload.data}`
      );

      this.scrollTerminal();
    });
  }

  // =============================
  // SEND DATA
  // =============================
  async send() {

    if (!this.msg.trim()) return;

    this.logs.push(`TX: ${this.msg}`);

    await this.serial.send(this.msg);

    this.msg = '';

    this.scrollTerminal();
  }

  // =============================
  // AUTO SCROLL
  // =============================
  scrollTerminal() {

    setTimeout(() => {

      const el =
        document.querySelector('.terminal');

      if (el) {
        el.scrollTop = el.scrollHeight;
      }

    }, 50);
  }

  toggleHex() {
    this.hex = !this.hex;
  }

}
