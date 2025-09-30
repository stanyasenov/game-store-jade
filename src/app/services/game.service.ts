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

// This service handles all communication with our Spring Boot backend
// Think of it as the middleman between our Angular frontend and the Java API
@Injectable({
  providedIn: 'root'
})
export class GameService {
  // This is where our Spring Boot API is running - change the port if needed
  private apiUrl = 'http://localhost:8080/api/games';

  // Angular automatically gives us an HTTP client to make web requests
  constructor(private http: HttpClient) { }

  // Search for a specific game by title
  // This talks to our JADE agents through the Spring Boot API
  searchGame(title: string): Observable<GameSearchResponse> {
    // Create URL parameters (like ?title=GameName in the web address)
    const params = new HttpParams().set('title', title);
    
    // Make the actual web request to our backend
    return this.http.get<GameSearchResponse>(`${this.apiUrl}/search`, { params })
      .pipe(
        // If something goes wrong, handle it gracefully instead of crashing
        catchError(this.handleError<GameSearchResponse>('searchGame'))
      );
  }

  // Get all games from just the SQLite database
  // This goes through the GameStop JADE agent
  getAllGamesFromSQLite(): Observable<GameListResponse> {
    return this.http.get<GameListResponse>(`${this.apiUrl}/sqlite/all`)
      .pipe(
        catchError(this.handleError<GameListResponse>('getAllGamesFromSQLite'))
      );
  }

  // Get all games from just the Ontology database
  // This goes through the Distributor JADE agent
  getAllGamesFromOntology(): Observable<GameListResponse> {
    return this.http.get<GameListResponse>(`${this.apiUrl}/ontology/all`)
      .pipe(
        catchError(this.handleError<GameListResponse>('getAllGamesFromOntology'))
      );
  }

  // Get games from both databases - this is the raw data from the backend
  // The backend already combines them, but doesn't handle duplicates smartly
  getAllGamesFromBoth(): Observable<CombinedGameListResponse> {
    return this.http.get<CombinedGameListResponse>(`${this.apiUrl}/all`)
      .pipe(
        catchError(this.handleError<CombinedGameListResponse>('getAllGamesFromBoth'))
      );
  }

  // This is our smart version that handles duplicate games properly
  // Instead of showing the same game twice, it merges them intelligently
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

        // Extract the game lists from each database (with fallback to empty arrays)
        const sqliteGames = response.sqliteGames || [];
        const ontologyGames = response.ontologyGames || [];
        
        // Here's where the magic happens - merge duplicates intelligently
        const enhancedGames = this.mergeAndEnhanceGames(sqliteGames, ontologyGames);
        
        // Count how many games appear in both databases
        const duplicateCount = enhancedGames.filter(game => game.sources.length > 1).length;

        // Send back all the processed data
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
  // This is where we decide if two games are the same and merge their data
  private mergeAndEnhanceGames(sqliteGames: Game[], ontologyGames: Game[]): EnhancedGame[] {
    // Think of this Map as a smart filing cabinet where we store games by their "key"
    // The key is based on the game title, so games with the same title get filed together
    const gameMap = new Map<string, EnhancedGame>();

    // First, process all the SQLite games
    sqliteGames.forEach(game => {
      // Create a unique key for this game (basically a cleaned-up version of the title)
      const key = this.createGameKey(game);
      
      // Create an enhanced version of this game with extra tracking info
      const enhancedGame: EnhancedGame = {
        ...game, // Copy all the original game data
        sources: ['SQLite'], // Track that this came from SQLite
        sqliteData: { ...game }, // Keep a copy of the SQLite version
        hasDifferences: false, // No differences yet since it's only from one source
        differences: []
      };
      
      // File it away in our smart cabinet
      gameMap.set(key, enhancedGame);
    });

    // Now process all the Ontology games
    ontologyGames.forEach(game => {
      const key = this.createGameKey(game);
      
      // Check if we already have this game from SQLite
      if (gameMap.has(key)) {
        // We found a duplicate! Time to merge the data
        const existingGame = gameMap.get(key)!;
        
        // Add Ontology as a source for this game
        existingGame.sources.push('Ontology');
        
        // Keep a copy of the Ontology version for comparison
        existingGame.ontologyData = { ...game };
        
        // Check if the two versions have different information
        const differences = this.detectDifferences(existingGame.sqliteData!, game);
        existingGame.hasDifferences = differences.length > 0;
        existingGame.differences = differences;
        
        // Merge the data - use the first non-empty value we find
        // This way if SQLite has a description but Ontology doesn't, we keep SQLite's
        // But if SQLite is missing something and Ontology has it, we use Ontology's
        existingGame.description = existingGame.description || game.description;
        existingGame.genre = existingGame.genre || game.genre;
        existingGame.platform = existingGame.platform || game.platform;
        existingGame.price = existingGame.price ?? game.price; // ?? handles 0 prices correctly
        existingGame.developer = existingGame.developer || game.developer;
        existingGame.publisher = existingGame.publisher || game.publisher;
        existingGame.releaseDate = existingGame.releaseDate || game.releaseDate;
        
      } else {
        // This game only exists in Ontology, not in SQLite
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

    // Convert our filing cabinet back to a regular list and sort alphabetically
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

  // Create a standardized key for identifying duplicate games
  // This cleans up the title so "Super Mario Bros" and "super mario bros  " are treated as the same
  private createGameKey(game: Game): string {
    return game.title.toLowerCase().trim().replace(/\s+/g, ' ');
  }

  // Compare two versions of the same game and find what's different
  // This helps us show users when the same game has conflicting info in different databases
  private detectDifferences(sqliteGame: Partial<Game>, ontologyGame: Game): string[] {
    const differences: string[] = [];
    
    // These are the fields we care about comparing
    const fieldsToCompare = ['description', 'genre', 'platform', 'price', 'developer', 'publisher', 'releaseDate'];

    fieldsToCompare.forEach(field => {
      // Get the value from each database
      const sqliteValue = (sqliteGame as any)[field];
      const ontologyValue = (ontologyGame as any)[field];
      
      // If both databases have a value for this field, but they're different, mark it as a difference
      if (sqliteValue && ontologyValue && sqliteValue !== ontologyValue) {
        differences.push(field);
      }
    });

    return differences;
  }

  // Generic error handler that prevents the app from crashing when something goes wrong
  // Instead of breaking, it logs the error and returns a safe fallback value
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      // Log the error so we can debug it later
      console.error(`${operation} failed:`, error);
      
      // Return a safe fallback value so the app keeps working
      return of(result as T);
    };
  }
}