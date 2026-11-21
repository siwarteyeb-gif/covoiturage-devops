import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { HomeComponent } from './features/home/home.component';

const T = ' · Covoiturage Étudiant';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'Accueil' + T },
  {
    path: 'login', canActivate: [guestGuard], title: 'Connexion' + T,
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register', canActivate: [guestGuard], title: 'Inscription' + T,
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'profile', canActivate: [authGuard], title: 'Mon profil' + T,
    loadComponent: () => import('./features/profile/profile.component').then((m) => m.ProfileComponent),
  },
  {
    path: 'vehicles', canActivate: [authGuard], title: 'Mes véhicules' + T,
    loadComponent: () => import('./features/vehicles/vehicles.component').then((m) => m.VehiclesComponent),
  },
  // Attention : les routes fixes (search, new, mine) doivent précéder /trips/:id
  {
    path: 'trips/search', canActivate: [authGuard], title: 'Rechercher un trajet' + T,
    loadComponent: () => import('./features/trips/trip-search.component').then((m) => m.TripSearchComponent),
  },
  {
    path: 'trips/new', canActivate: [authGuard], title: 'Publier un trajet' + T,
    loadComponent: () => import('./features/trips/trip-publish.component').then((m) => m.TripPublishComponent),
  },
  {
    path: 'trips/mine', canActivate: [authGuard], title: 'Mes trajets' + T,
    loadComponent: () => import('./features/trips/my-trips.component').then((m) => m.MyTripsComponent),
  },
  {
    path: 'trips/:id', canActivate: [authGuard], title: 'Détail du trajet' + T,
    loadComponent: () => import('./features/trips/trip-detail.component').then((m) => m.TripDetailComponent),
  },
  {
    path: 'bookings', canActivate: [authGuard], title: 'Mes réservations' + T,
    loadComponent: () => import('./features/bookings/bookings.component').then((m) => m.BookingsComponent),
  },
  {
    path: 'admin', canActivate: [authGuard, adminGuard], title: 'Administration' + T,
    loadComponent: () =>
      import('./features/admin/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
  },
  { path: '**', redirectTo: '' },
];
