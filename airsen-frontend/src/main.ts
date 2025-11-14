import { platformBrowserDynamic } from "@angular/platform-browser-dynamic";
import { AppModule } from "./app/app.module";
import * as L from "leaflet";

// Fix Leaflet default icon paths
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "assets/leaflet/marker-icon-2x.png",
  iconUrl: "assets/leaflet/marker-icon.png",
  shadowUrl: "assets/leaflet/marker-shadow.png",
});

platformBrowserDynamic()
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
