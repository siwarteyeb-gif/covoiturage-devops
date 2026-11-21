import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.models';

const TOKEN_KEY = 'covoiturage.token';
const USER_KEY = 'covoiturage.user';

/** Lit la date d'expiration (claim "exp") d'un JWT, sans vérifier la signature (c'est le rôle du back-end). */
function isTokenExpired(token: string): boolean {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const { exp } = JSON.parse(json) as { exp?: number };
    return !exp || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(null);
  private readonly _user = signal<User | null>(null);

  readonly token = this._token.asReadonly();
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._token() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  constructor() {
    this.restoreSession();
  }

  /** F01 : inscription (l'utilisateur est connecté directement). */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_URL}/auth/register`, request)
      .pipe(tap((response) => this.setSession(response)));
  }

  /** F02 : connexion. */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_URL}/auth/login`, request)
      .pipe(tap((response) => this.setSession(response)));
  }

  /** F04 : déconnexion (le token JWT est supprimé côté client). */
  logout(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  /** Supprime la session sans redirection (utilisé par l'intercepteur sur une erreur 401). */
  clearSession(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } catch {
      // stockage indisponible : on ignore
    }
    this._token.set(null);
    this._user.set(null);
  }

  /** Met à jour l'utilisateur courant (après modification du profil). */
  updateUser(user: User): void {
    this._user.set(user);
    try {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch {
      // ignoré
    }
  }

  private setSession(response: AuthResponse): void {
    try {
      localStorage.setItem(TOKEN_KEY, response.token);
      localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    } catch {
      // ignoré : la session reste valable en mémoire
    }
    this._token.set(response.token);
    this._user.set(response.user);
  }

  private restoreSession(): void {
    try {
      const token = localStorage.getItem(TOKEN_KEY);
      const userJson = localStorage.getItem(USER_KEY);
      if (token && userJson && !isTokenExpired(token)) {
        this._token.set(token);
        this._user.set(JSON.parse(userJson) as User);
      } else if (token || userJson) {
        this.clearSession();
      }
    } catch {
      this.clearSession();
    }
  }
}
