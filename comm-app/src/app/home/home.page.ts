import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { Serial } from '../services/serial';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false,
})

export class HomePage {

  devices: any[] = [];
  selectedBaud = 115200;
  connecting = false;

  baudRates = [
    9600,
    19200,
    38400,
    57600,
    115200,
    230400,
    460800
  ];

  constructor(
    private router: Router,
    private serial: Serial
  ) { }

  refresh() {
    // Android handles real detection automatically
    this.devices = [
      { name: 'USB Device (STM32/Arduino/CH340)' }
    ];
  }

  async connect() {
    this.connecting = true;

    try {
      await this.serial.connect(this.selectedBaud);

      this.router.navigate(['/terminal'], {
        state: { baud: this.selectedBaud }
      });

    } catch (e) {
      console.error(e);
    }

    this.connecting = false;
  }
}
