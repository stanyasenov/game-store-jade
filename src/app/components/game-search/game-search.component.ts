import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { GameService } from '../../services/game.service';
import { Game, GameSearchResponse } from '../../models/game.model';

@Component({
  selector: 'app-game-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './game-search.component.html',
  styleUrls: ['./game-search.component.css']
})
export class GameSearchComponent {
  searchForm: FormGroup;
  searchResult: GameSearchResponse | null = null;
  isSearching: boolean = false;
  searchError: string = '';

  constructor(
    private fb: FormBuilder,
    private gameService: GameService
  ) {
    this.searchForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  onSearch(): void {
    if (this.searchForm.valid) {
      this.isSearching = true;
      this.searchError = '';
      this.searchResult = null;

      const title = this.searchForm.get('title')?.value;

      this.gameService.searchGame(title).subscribe({
        next: (response) => {
          this.searchResult = response;
          this.isSearching = false;
        },
        error: (error) => {
          this.searchError = 'Error searching for game. Please try again.';
          this.isSearching = false;
          console.error('Search error:', error);
        }
      });
    }
  }

  clearSearch(): void {
    this.searchResult = null;
    this.searchError = '';
    this.searchForm.reset();
  }
}
