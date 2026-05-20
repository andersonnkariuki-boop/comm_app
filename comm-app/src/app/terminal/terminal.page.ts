import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-terminal',
  templateUrl: './terminal.page.html',
  styleUrls: ['./terminal.page.scss'],
  standalone: false,
})
export class TerminalPage implements OnInit {
  message = '';
  logs: string[] = [];

  constructor() { }

  ngOnInit() {
  }

  ionViewWillEnter() {
    const nav = history.state;
    this.logs.push('Connected to: ' + (nav?.device?.name || 'Unknown'));
  }

  send() {
    this.logs.push('TX: ' + this.message);
    this.message = '';
  }

}
