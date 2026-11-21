export interface Vehicle {
  id: number;
  brand: string;
  model: string;
  color: string | null;
  seats: number;
}

export interface VehicleRequest {
  brand: string;
  model: string;
  color: string | null;
  seats: number;
}
