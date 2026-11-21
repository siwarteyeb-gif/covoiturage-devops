import { bookingBadge, formatDuration, tripBadge } from './labels';

describe('labels', () => {
  it('formats durations', () => {
    expect(formatDuration(25)).toBe('25 min');
    expect(formatDuration(65)).toBe('1 h 05');
  });

  it('gives a badge for each trip and booking status', () => {
    expect(tripBadge('FULL').text).toBe('Complet');
    expect(tripBadge('CANCELLED').tone).toBe('r');
    expect(bookingBadge('PENDING').text).toBe('En attente');
    expect(bookingBadge('ACCEPTED').tone).toBe('g');
  });
});
