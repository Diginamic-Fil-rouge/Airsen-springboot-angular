export interface AirQuality {
    id: number;
    communeInseeCode: string,
    communeName: string,
    departmentName: string,
    regionName: string,
    measurementDate: Date,
    atmoIndex: number,
    qualifier: string,
    color: string,
    no2Concentration: number,
    o3Concentration: number,
    pm10Concentration: number,
    pm25Concentration: number,
    so2Concentration: number,
    createdAt: Date
}