import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'search',
    loadComponent: () => import('./components/game-search/game-search.component').then(m => m.GameSearchComponent)
  },
  {
    path: 'games',
    loadComponent: () => import('./components/game-list/game-list.component').then(m => m.GameListComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];