import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthResponse } from '../models/auth.models';
import { AuthService } from './auth.service';

function fakeJwt(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ sub: 'karim@test.tn', exp: expSeconds }));
  return `header.${payload}.signature`;
}

const user = {
  id: 1,
  name: 'Karim',
  email: 'karim@test.tn',
  phone: '22123456',
  role: 'USER' as const,
  averageRating: 0,
  createdAt: '2026-11-01T10:00:00',
};

function setup(): { service: AuthService; http: HttpTestingController } {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
  });
  return { service: TestBed.inject(AuthService), http: TestBed.inject(HttpTestingController) };
}

describe('AuthService', () => {
  beforeEach(() => localStorage.clear());

  it('login stores the session', () => {
    const { service, http } = setup();
    const token = fakeJwt(Math.floor(Date.now() / 1000) + 3600);
    const response: AuthResponse = { token, tokenType: 'Bearer', expiresInMs: 3600000, user };

    service.login({ email: 'karim@test.tn', password: 'Password1' }).subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(response);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.user()?.email).toBe('karim@test.tn');
    expect(localStorage.getItem('covoiturage.token')).toBe(token);
  });

  it('clearSession removes the session', () => {
    const { service, http } = setup();
    const token = fakeJwt(Math.floor(Date.now() / 1000) + 3600);
    service.login({ email: 'karim@test.tn', password: 'Password1' }).subscribe();
    http.expectOne('/api/auth/login').flush({ token, tokenType: 'Bearer', expiresInMs: 1, user });

    service.clearSession();

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('covoiturage.token')).toBeNull();
  });

  it('ignores an expired token stored in localStorage', () => {
    localStorage.setItem('covoiturage.token', fakeJwt(1));
    localStorage.setItem('covoiturage.user', JSON.stringify(user));

    const { service } = setup();

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('covoiturage.token')).toBeNull();
  });

  it('restores a valid token stored in localStorage', () => {
    localStorage.setItem('covoiturage.token', fakeJwt(Math.floor(Date.now() / 1000) + 3600));
    localStorage.setItem('covoiturage.user', JSON.stringify(user));

    const { service } = setup();

    expect(service.isAuthenticated()).toBe(true);
    expect(service.user()?.name).toBe('Karim');
  });
});
