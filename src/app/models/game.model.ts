export interface Game {
    id?: number;
    title: string;
    genre?: string;
    platform?: string;
    price?: number;
    description?: string;
    source?: string;
    releaseDate?: string;
    developer?: string;
    publisher?: string;
    uniqueId?: string;
    isDuplicate?: boolean;
    duplicateSources?: string[];
  }
  
  export interface GameSearchResponse {
    game: Game;
    source: string;
    message: string;
  }
  
  export interface GameListResponse {
    games: Game[];
    source: string;
    message: string;
  }
  
  export interface CombinedGameListResponse {
    allGames: Game[];
    sqliteGames: Game[];
    ontologyGames: Game[];
    message: string;
  }
  
  export interface EnhancedGame extends Game {
    sources: string[];
    sqliteData?: Partial<Game>;
    ontologyData?: Partial<Game>;
    hasDifferences?: boolean;
    differences?: string[];
  }