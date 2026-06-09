import ApexCharts from 'apexcharts';

interface BalancePoint {
    x: number;
    y: number;
}

/**
 * Custom element rendering a balance-over-time area chart with ApexCharts. The server pushes data via the
 * {@link setSeries} method (called through Flow's callJsFunction).
 */
class BalanceChartElement extends HTMLElement {
    private chart?: ApexCharts;
    private container?: HTMLDivElement;
    private data: BalancePoint[] = [];
    private seriesName = 'Balance';

    connectedCallback(): void {
        if (!this.container) {
            this.style.display = 'block';
            this.style.width = '100%';
            this.container = document.createElement('div');
            this.container.style.width = '100%';
            this.appendChild(this.container);
        }
        this.renderChart();
    }

    disconnectedCallback(): void {
        this.chart?.destroy();
        this.chart = undefined;
    }

    /** Invoked from the server via callJsFunction('setSeries', json, title). */
    setSeries(json: string, title: string): void {
        this.data = JSON.parse(json) as BalancePoint[];
        this.seriesName = title && title.length > 0 ? title : 'Balance';
        this.renderChart();
    }

    private renderChart(): void {
        if (!this.container) {
            return;
        }

        const fmt = (val: number): string => `€ ${Number(val).toFixed(2)}`;

        const options: any = {
            chart: {
                type: 'area',
                height: 300,
                fontFamily: 'inherit',
                toolbar: { show: true },
                zoom: { enabled: true },
                animations: { enabled: true },
            },
            series: [{ name: this.seriesName, data: this.data }],
            xaxis: { type: 'datetime' },
            yaxis: { labels: { formatter: fmt } },
            dataLabels: { enabled: false },
            stroke: { curve: 'stepline', width: 2 },
            fill: { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05 } },
            tooltip: { x: { format: 'yyyy-MM-dd' }, y: { formatter: fmt } },
            colors: ['var(--lumo-primary-color, #1676f3)'],
            noData: { text: 'No data for the selected range' },
        };

        if (this.chart) {
            this.chart.updateOptions(options);
        } else {
            this.chart = new ApexCharts(this.container, options);
            this.chart.render();
        }
    }
}

if (!customElements.get('balance-chart')) {
    customElements.define('balance-chart', BalanceChartElement);
}
