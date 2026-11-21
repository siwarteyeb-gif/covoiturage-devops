import { Component, ElementRef, OnDestroy, AfterViewInit, effect, input, viewChild } from '@angular/core';
import * as L from 'leaflet';
// Leaflet : la bibliothèque qui dessine la carte. "L" contient toutes ses fonctions (L.map, L.polyline...).
/**
 * Carte Leaflet (fond OpenStreetMap) qui affiche un itinéraire et/ou les points de départ et d'arrivée.
 * Les points sont des couples [latitude, longitude].
 */
//Dessiner le trait (L.polyline)
//Zoomer sur le trajet (fitBounds)
//Dessiner les points de départ et d’arrivée (L.circleMarker)
//Afficher le fond de carte (routes, villes, noms)	OpenStreetMap, que Leaflet télécharge et affiche
@Component({
  selector: 'app-route-map',
  template: `<div #mapEl class="map" role="img" aria-label="Carte de l'itinéraire"></div>`,
})
export class RouteMapComponent implements AfterViewInit, OnDestroy {
  readonly route = input<number[][] | null>(null);
  readonly start = input<[number, number] | null>(null);
  readonly end = input<[number, number] | null>(null);

  private readonly mapEl = viewChild.required<ElementRef<HTMLDivElement>>('mapEl');
  private map: L.Map | null = null;
  private readonly layer = L.layerGroup();

  constructor() {
    // Redessine la carte à chaque changement des entrées
    effect(() => {
      this.route();
      this.start();
      this.end();
      this.draw();
    });
  }

  ngAfterViewInit(): void {
    this.map = L.map(this.mapEl().nativeElement, { zoomControl: true }).setView([34.0, 9.5], 6);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap',
    }).addTo(this.map);
    this.layer.addTo(this.map);
    this.draw();
    // Le conteneur peut avoir sa taille finale après le premier affichage
    setTimeout(() => {
      this.map?.invalidateSize();
      this.draw();
    }, 0);
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
  }

  private draw(): void {
    if (!this.map) return;
    this.layer.clearLayers();

    const route = this.route();
    const start = this.start();
    const end = this.end();
    const bounds: L.LatLngExpression[] = [];

    if (route && route.length > 1) {
      const points = route.map((p) => [p[0], p[1]] as L.LatLngTuple);
      L.polyline(points, { color: '#0f766e', weight: 5, opacity: 0.9 }).addTo(this.layer);
      bounds.push(...points);
    } else if (start && end) {
      L.polyline([start, end], { color: '#0f766e', weight: 4, dashArray: '8 8' }).addTo(this.layer);
    }
    if (start) {
      this.marker(start, '#0f766e', 'Départ');
      bounds.push(start);
    }
    if (end) {
      this.marker(end, '#b45309', 'Arrivée');
      bounds.push(end);
    }
    if (bounds.length > 0) {
      this.map.fitBounds(L.latLngBounds(bounds), { padding: [30, 30], maxZoom: 14 });
    }
  }

  private marker(point: [number, number], color: string, label: string): void {
    L.circleMarker(point, { radius: 9, color: '#ffffff', weight: 3, fillColor: color, fillOpacity: 1 })
      .bindTooltip(label)
      .addTo(this.layer);
  }
}
