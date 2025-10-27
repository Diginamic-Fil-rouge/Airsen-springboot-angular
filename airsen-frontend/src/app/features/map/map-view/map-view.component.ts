import { Component, AfterViewInit, input, inject, Output, EventEmitter, ViewContainerRef, Injector, ComponentRef, createComponent } from '@angular/core';
import * as L from 'leaflet';
import * as I from './icons';
import { Observable } from 'rxjs';
import { Commune } from '../models/commune.model';
import { GeographicService } from '../services/geographic.service';
import { MarkerPopupComponent } from './popup/markerPopup.component';
@Component({
  standalone: false,
  selector: 'app-map-view',
  templateUrl: './map-view.component.html',
  styleUrls: ['./map-view.component.scss']
})
export class MapViewComponent implements AfterViewInit {
  private map: any;
  private geographicService = inject(GeographicService)

  markersInitialized = false;
  communes = input<Observable<Commune[]>>();
  communeClicked = input<Commune | null>();
  @Output() onMarkerClick = new EventEmitter<any>();
  @Output() anchorLinkClicked = new EventEmitter<any>();

  constructor(
    private viewContainerRef: ViewContainerRef,
    private injector: Injector
  ) { }

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnChanges() {
    this.initMarkers();
  }

  private initMap(): void {
    this.map = L.map('map', {
      // coordinates of France
      center: [47.0, 1.5231],
      zoom: 7
    });

    const tiles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      minZoom: 3,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    });

    tiles.addTo(this.map);
  }

  initMarkers() {
    if (!this.markersInitialized) {
      this.communes()?.forEach(communes => {
        console.log("commune : ", communes);
        communes.forEach(commune => {
          this.addMarketToMap(commune);
        });
      });
      this.markersInitialized = true;
    }

    if (this.communeClicked()) {
      this.addMarketToMap(this.communeClicked());
    }
  }

  addMarketToMap(commune: any) {
    if (!commune.latitude || !commune.longitude) {
      return;
    }

    let icon: any = null;

    if (commune.qualifier === 'Bon') {
      icon = I.goodIcon;
    } else if (commune.qualifier === 'Moyen') {
      icon = I.mediumIcon;
    }
    else if (commune.qualifier === 'Dégradé') {
      icon = I.deterioratedIcon;
    }
    else if (commune.qualifier === 'Mauvais') {
      icon = I.badIcon;
    }
    else if (commune.qualifier === 'Très mauvais') {
      icon = I.veryBadIcon;
    }
    else if (commune.qualifier === 'Extrêmement mauvais') {
      icon = I.extremelyBadIcon;
    }
    else {
      icon = I.undefinedIcon;
    }

    const marker = L.marker([commune.latitude, commune.longitude], { icon: icon }).addTo(this.map);

    marker.on('click', () => {
      this.onMarkerClick.emit(commune);
      this.openPopup(marker, commune);
    });
  }

  openPopup(marker: L.Marker, commune: Commune) {
    // Create DOM container for popup content
    const popupContainer = document.createElement('div');

    // Create dynamic Angular component **rendered into** popupContainer
    const compRef = this.viewContainerRef.createComponent(MarkerPopupComponent, {
      injector: this.injector,
    });
    popupContainer.appendChild(compRef.location.nativeElement);

    // pass inputs
    compRef.instance.commune = commune;

    // subscribe to output and forward to parent
    const sub = compRef.instance.more.subscribe(() => {
      this.anchorLinkClicked.emit(commune);
    });

    // open popup
    var popup = L.popup()
    .setLatLng([commune.latitude, commune.longitude])
    .setContent(popupContainer)
    .openOn(this.map);

    // Destroy dynamic component when popup closes (prevents leaks)
    const onClose = () => {
      try {
        sub.unsubscribe();
      } catch { }
      try {
        compRef.destroy();
      } catch { }
      marker.off('popupclose', onClose);
    };

    marker.on('popupclose', onClose);
  }

}
