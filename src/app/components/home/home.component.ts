import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { GameService } from '../../services/game.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  apiHealth: string = 'Checking...';

  constructor(private gameService: GameService) { }

  ngOnInit(): void {
    this.checkApiHealth();
  }

  checkApiHealth(): void {
    this.gameService.healthCheck().subscribe({
      next: (response) => {
        this.apiHealth = response || 'API connection failed';
      },
      error: (error) => {
        this.apiHealth = 'API connection failed: ' + error.message;
        console.error('API health check failed:', error);
      }
    });
  }
}