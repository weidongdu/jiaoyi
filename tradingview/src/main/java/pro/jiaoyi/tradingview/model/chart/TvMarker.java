package pro.jiaoyi.tradingview.model.chart;

import lombok.Data;

@Data
public class TvMarker {
    private String time;
    private String text;
    private String color;
    //SeriesMarkerPosition
    //Ƭ SeriesMarkerPosition: "aboveBar" | "belowBar" | "inBar"
    private String position;


    //SeriesMarkerShape
    //Ƭ SeriesMarkerShape: "circle" | "square" | "arrowUp" | "arrowDown"
    private String shape;
}
