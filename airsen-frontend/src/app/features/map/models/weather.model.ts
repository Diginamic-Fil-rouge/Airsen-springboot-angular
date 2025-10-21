export interface Weather {
    id: number,
    communeId: number,
    communeName: string,
    inseeCode: string,
    measurementDate: Date,
    temperature: number,
    humidity: number,
    windSpeed: number,
    windDirection: number,
    weatherCode: number,
    createdAt: Date
}