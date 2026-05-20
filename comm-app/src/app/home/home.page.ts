import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false,
})

export class HomePage {

  devices: any[] = [];

  constructor(private router: Router) { }

  refresh() {
    // placeholder (Android will fill later)
    this.devices = [
      { name: 'STM32 Device' },
      { name: 'Arduino UNO' }
    ];
  }

  connect(device: any) {
    this.router.navigate(['/terminal'], {
      state: { device }
    });
  }

}
