import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { 
  Game, 
  GameSearchResponse, 
  GameListResponse, 
  CombinedGameListResponse,
  EnhancedGame 
} from '../models/game.model';

@Injectable({
  providedIn: 'root'
})
export class GameService {

  private apiUrl = 'http://localhost:8080/api/games';

  constructor(private http: HttpClient) { }

  searchGame(title: string): Observable<GameSearchResponse> {
    const params = new HttpParams().set('title', title);
    
    return this.http.get<GameSearchResponse>(`${this.apiUrl}/search`, { params })
      .pipe(
        catchError(this.handleError<GameSearchResponse>('searchGame'))
      );
  }

  // Get all games from just the SQLite database
  getAllGamesFromSQLite(): Observable<GameListResponse> {
    return this.http.get<GameListResponse>(`${this.apiUrl}/sqlite/all`)
      .pipe(
        catchError(this.handleError<GameListResponse>('getAllGamesFromSQLite'))
      );
  }

  // Get all games from just the Ontology database
  getAllGamesFromOntology(): Observable<GameListResponse> {
    return this.http.get<GameListResponse>(`${this.apiUrl}/ontology/all`)
      .pipe(
        catchError(this.handleError<GameListResponse>('getAllGamesFromOntology'))
      );
  }

  // Get games from both databases
  getAllGamesFromBoth(): Observable<CombinedGameListResponse> {
    return this.http.get<CombinedGameListResponse>(`${this.apiUrl}/all`)
      .pipe(
        catchError(this.handleError<CombinedGameListResponse>('getAllGamesFromBoth'))
      );
  }

  getEnhancedGamesFromBoth(): Observable<{
    enhancedGames: EnhancedGame[];
    sqliteGames: Game[];
    ontologyGames: Game[];
    message: string;
    duplicateCount: number;
  }> {
    // First get the raw data from both databases
    return this.getAllGamesFromBoth().pipe(
      // Then process that data to make it smarter
      map(response => {
        // Safety check - make sure we actually got data back
        if (!response) {
          return {
            enhancedGames: [],
            sqliteGames: [],
            ontologyGames: [],
            message: 'No data received',
            duplicateCount: 0
          };
        }

        // Extract the game lists from each database
        const sqliteGames = response.sqliteGames || [];
        const ontologyGames = response.ontologyGames || [];
        
        // merge duplicates 
        const enhancedGames = this.mergeAndEnhanceGames(sqliteGames, ontologyGames);
        
        const duplicateCount = enhancedGames.filter(game => game.sources.length > 1).length;

        return {
          enhancedGames,
          sqliteGames,
          ontologyGames,
          message: response.message + ` (${duplicateCount} duplicates detected)`,
          duplicateCount
        };
      })
    );
  }

  // The core logic for handling duplicate games
  private mergeAndEnhanceGames(sqliteGames: Game[], ontologyGames: Game[]): EnhancedGame[] {
    const gameMap = new Map<string, EnhancedGame>();

    //process all SQLite games
    sqliteGames.forEach(game => {
      // Create a unique key for this game 
      const key = this.createGameKey(game);
      
      // Create an enhanced version of this game with extra tracking info
      const enhancedGame: EnhancedGame = {
        ...game, 
        sources: ['SQLite'], 
        sqliteData: { ...game }, 
        hasDifferences: false,
        differences: []
      };
      
      gameMap.set(key, enhancedGame);
    });

    // process all the Ontology games
    ontologyGames.forEach(game => {
      const key = this.createGameKey(game);
      
      // Check if we already have this game from SQLite
      if (gameMap.has(key)) {
        const existingGame = gameMap.get(key)!;
        
        existingGame.sources.push('Ontology');
        
        existingGame.ontologyData = { ...game };
        
        const differences = this.detectDifferences(existingGame.sqliteData!, game);
        existingGame.hasDifferences = differences.length > 0;
        existingGame.differences = differences;
        
        existingGame.description = existingGame.description || game.description;
        existingGame.genre = existingGame.genre || game.genre;
        existingGame.platform = existingGame.platform || game.platform;
        existingGame.price = existingGame.price ?? game.price; 
        existingGame.developer = existingGame.developer || game.developer;
        existingGame.publisher = existingGame.publisher || game.publisher;
        existingGame.releaseDate = existingGame.releaseDate || game.releaseDate;
        
      } else {
        const enhancedGame: EnhancedGame = {
          ...game,
          sources: ['Ontology'],
          ontologyData: { ...game },
          hasDifferences: false,
          differences: []
        };
        gameMap.set(key, enhancedGame);
      }
    });

    return Array.from(gameMap.values()).sort((a, b) => 
      a.title.toLowerCase().localeCompare(b.title.toLowerCase())
    );
  }

  // Simple health check to see if our backend API is alive
  healthCheck(): Observable<string> {
    return this.http.get(`${this.apiUrl}/health`, { responseType: 'text' })
      .pipe(
        catchError(this.handleError<string>('healthCheck'))
      );
  }

  private createGameKey(game: Game): string {
    return game.title.toLowerCase().trim().replace(/\s+/g, ' ');
  }

  private detectDifferences(sqliteGame: Partial<Game>, ontologyGame: Game): string[] {
    const differences: string[] = [];
    
    const fieldsToCompare = ['description', 'genre', 'platform', 'price', 'developer', 'publisher', 'releaseDate'];

    fieldsToCompare.forEach(field => {
      const sqliteValue = (sqliteGame as any)[field];
      const ontologyValue = (ontologyGame as any)[field];
      
      if (sqliteValue && ontologyValue && sqliteValue !== ontologyValue) {
        differences.push(field);
      }
    });

    return differences;
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      
      return of(result as T);
    };
  }
}
