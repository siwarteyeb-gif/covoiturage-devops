import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TripApiService } from './trip-api.service';

describe('TripApiService', () => {
  let service: TripApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TripApiService);
    http = TestBed.inject(HttpTestingController);
  });

  it('search sends only the filled parameters', () => {
    service.search({ from: 'Ariana', seats: 2, maxPrice: undefined, date: '' }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/trips/search');
    expect(req.request.params.get('from')).toBe('Ariana');
    expect(req.request.params.get('seats')).toBe('2');
    expect(req.request.params.has('maxPrice')).toBe(false);
    expect(req.request.params.has('date')).toBe(false);
    req.flush([]);
  });

  it('changeStatus sends a PATCH with the new status', () => {
    service.changeStatus(5, 'IN_PROGRESS').subscribe();

    const req = http.expectOne('/api/trips/5/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'IN_PROGRESS' });
    req.flush({});
  });
});
