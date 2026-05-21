import { Component, OnInit } from '@angular/core';
import { Serial } from '../services/serial';
import { Router } from '@angular/router';


@Component({
  selector: 'app-terminal',
  templateUrl: './terminal.page.html',
  styleUrls: ['./terminal.page.scss'],
  standalone: false,
})
export class TerminalPage implements OnInit {
  logs: string[] = [];
  message = '';
  hex = false;

  private listener: any;

  constructor(
    private serial: Serial,
    private router: Router
  ) { }

  ngOnInit() {

    this.listener = (payload: any) => {

      const time = new Date(payload.timestamp).toLocaleTimeString();

      this.logs.push(`[${time}] RX: ${payload.data}`);

      setTimeout(() => this.autoScroll(), 50);
    };

    window.addEventListener('serialData', this.listener);

  }

  send() {
    if (!this.message.trim()) return;

    const time = new Date().toLocaleTimeString();

    this.logs.push(`[${time}] TX: ${this.message}`);

    this.serial.send(this.message);

    this.message = '';

    setTimeout(() => this.autoScroll(), 50);
  }

  toggleHex() {
    this.hex = !this.hex;
  }

  clear() {
    this.logs = [];
  }

  autoScroll() {
    const el = document.querySelector('.terminal');
    el?.scrollTo(0, el.scrollHeight);
  }

  ngOnDestroy() {
    if (this.listener) {
      window.removeEventListener('serialData', this.listener);
    }
  }
}
