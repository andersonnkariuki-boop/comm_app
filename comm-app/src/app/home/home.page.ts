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

  baudrate = 115200;

  constructor(
    private router: Router,
    private serial: Serial
  ) { }

   async connect() {

    await this.serial.connect(this.baudrate);

    this.router.navigate(['/terminal']);
  }

}
