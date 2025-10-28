import { Component, Input } from "@angular/core";

@Component({
  standalone: false,
  selector: "app-map-datas",
  templateUrl: "./map-datas.component.html",
  styleUrls: ["./map-datas.component.scss"],
})
export class MapDatasComponent {
  @Input() airQualityClicked: any;
  @Input() weatherClicked: any;
}
