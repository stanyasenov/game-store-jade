import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';
import { Game, EnhancedGame } from '../../models/game.model';

@Component({
  selector: 'app-game-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-list.component.html',
  styleUrls: ['./game-list.component.css']
})
export class GameListComponent implements OnInit {
  enhancedGames: EnhancedGame[] = [];
  sqliteGames: Game[] = [];
  ontologyGames: Game[] = [];
  
  currentView: 'enhanced' | 'sqlite' | 'ontology' | 'duplicates' = 'enhanced';
  isLoading: boolean = false;
  error: string = '';
  message: string = '';
  duplicateCount: number = 0;

  constructor(private gameService: GameService) { }

  ngOnInit(): void {
    this.loadAllGames();
  }

  loadAllGames(): void {
    this.isLoading = true;
    this.error = '';

    this.gameService.getEnhancedGamesFromBoth().subscribe({
      next: (response) => {
        this.enhancedGames = response.enhancedGames;
        this.sqliteGames = response.sqliteGames;
        this.ontologyGames = response.ontologyGames;
        this.message = response.message;
        this.duplicateCount = response.duplicateCount;
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load games. Please try again later.';
        this.isLoading = false;
        console.error('Error loading games:', error);
      }
    });
  }

  getCurrentGames(): (Game | EnhancedGame)[] {
    switch (this.currentView) {
      case 'sqlite':
        return this.sqliteGames;
      case 'ontology':
        return this.ontologyGames;
      case 'duplicates':
        return this.enhancedGames.filter(game => game.sources.length > 1);
      default:
        return this.enhancedGames;
    }
  }

  getCurrentTitle(): string {
    switch (this.currentView) {
      case 'sqlite':
        return `SQLite Database (${this.sqliteGames.length} games)`;
      case 'ontology':
        return `Ontology Database (${this.ontologyGames.length} games)`;
      case 'duplicates':
        return `Duplicate Games (${this.duplicateCount} games)`;
      default:
        return `All Games Enhanced (${this.enhancedGames.length} total)`;
    }
  }

  setView(view: 'enhanced' | 'sqlite' | 'ontology' | 'duplicates'): void {
    this.currentView = view;
  }

  refresh(): void {
    this.loadAllGames();
  }

  isEnhancedGame(game: Game | EnhancedGame): game is EnhancedGame {
    return 'sources' in game;
  }

  getSourceBadgeClass(source: string): string {
    switch (source.toLowerCase()) {
      case 'sqlite':
        return 'bg-primary';
      case 'ontology':
        return 'bg-success';
      default:
        return 'bg-secondary';
    }
  }
}