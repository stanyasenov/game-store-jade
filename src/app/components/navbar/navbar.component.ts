import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { GameService } from '../../services/game.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  apiStatus: string = 'Checking...';

  constructor(private gameService: GameService) { }

  ngOnInit(): void {
    this.checkApiHealth();
  }

  checkApiHealth(): void {
    this.gameService.healthCheck().subscribe({
      next: (response) => {
        this.apiStatus = response ? 'Online' : 'Offline';
      },
      error: (error) => {
        this.apiStatus = 'Offline';
        console.error('API health check failed:', error);
      }
    });
  }
}