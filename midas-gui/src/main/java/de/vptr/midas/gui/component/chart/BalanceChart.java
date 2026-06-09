package de.vptr.midas.gui.component.chart;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

import de.vptr.midas.gui.dto.BalancePointDto;

/**
 * A balance-over-time line chart backed by ApexCharts. Data is pushed from the server as a JSON array of {@code {x:
 * epochMillis, y: amount}} points and rendered by the {@code balance-chart} web component.
 */
@Tag("balance-chart")
@NpmPackage(value = "apexcharts", version = "5.14.0")
@JsModule("./balance-chart.ts")
public class BalanceChart extends Component {

    private static final Logger LOG = Logger.getLogger(BalanceChart.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Initializes BalanceChart. */
    public BalanceChart() {
        this.getElement().getStyle().set("display", "block");
        this.getElement().getStyle().set("width", "100%");
    }

    /**
     * Replaces the chart's data with the given cumulative balance series.
     *
     * @param points
     *            the ordered balance-over-time points (date + cumulative balance)
     * @param seriesTitle
     *            the legend/series label, e.g. the account name or "Total Balance"
     */
    public void setPoints(final List<BalancePointDto> points, final String seriesTitle) {
        final List<Map<String, Object>> data = new ArrayList<>(points.size());
        for (final BalancePointDto point : points) {
            final var date = point.getDate();
            final var balance = point.getBalance();
            if (date == null || balance == null) {
                continue;
            }
            final Map<String, Object> entry = new LinkedHashMap<>(2);
            entry.put("x", date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
            entry.put("y", balance);
            data.add(entry);
        }

        try {
            final String json = OBJECT_MAPPER.writeValueAsString(data);
            this.getElement().callJsFunction("setSeries", json, seriesTitle);
        } catch (final JsonProcessingException e) {
            LOG.error("Failed to serialize balance chart data", e);
        }
    }
}
