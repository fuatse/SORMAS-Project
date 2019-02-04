/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.dashboard.surveillance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseClassification;
import de.symeda.sormas.api.caze.CaseCriteria;
import de.symeda.sormas.api.caze.DashboardCaseDto;
import de.symeda.sormas.api.caze.NewCaseDateType;
import de.symeda.sormas.api.disease.DiseaseBurdenDto;
import de.symeda.sormas.api.event.DashboardEventDto;
import de.symeda.sormas.api.outbreak.DashboardOutbreakDto;
import de.symeda.sormas.api.person.PresentCondition;
import de.symeda.sormas.api.task.DashboardTaskDto;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.CurrentUser;
import de.symeda.sormas.ui.dashboard.DashboardDataProvider;
import de.symeda.sormas.ui.dashboard.DiseaseBurdenGrid;
import de.symeda.sormas.ui.dashboard.diagram.AbstractEpiCurveComponent;
import de.symeda.sormas.ui.dashboard.diagram.EpiCurveGrouping;
import de.symeda.sormas.ui.highcharts.HighChart;
import de.symeda.sormas.ui.utils.CssStyles;

public class DiseaseDifferenceSurveillanceComponent extends VerticalLayout {

	private static final long serialVersionUID = 6582975657305031105L;

	private DashboardDataProvider dashboardDataProvider;
	private HighChart chart;

	public DiseaseDifferenceSurveillanceComponent(DashboardDataProvider dashboardDataProvider) {
		this.dashboardDataProvider = dashboardDataProvider;

		Label title = new Label("Difference in number of cases");
		CssStyles.style(title, CssStyles.H2, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE);

		Label subtitle = new Label("Compared to previous time period");

		chart = new HighChart();
		chart.setSizeFull();

		// layout
		setWidth(100, Unit.PERCENTAGE);
		// setHeight(400, Unit.PIXELS);

		addComponent(title);
		addComponent(subtitle);
		addComponent(chart);
		setExpandRatio(chart, 1);

		setMargin(true);
		setSpacing(false);
		setSizeFull();
	}

	public void refresh(int visibleDiseasesCount) {
		List<DiseaseBurdenDto> diseasesBurden = dashboardDataProvider.getDiseasesBurden();

		// data mockup: manipulate the data
//		diseasesBurden = mockDataUp(diseasesBurden);

		// sort, limit and filter
		diseasesBurden = diseasesBurden.stream()
									   .sorted((dto1, dto2) -> (int) (dto2.getCaseCount() - dto1.getCaseCount()))
									   .limit(visibleDiseasesCount)
									   //.filter((dto) -> dto.hasCount())
									   .collect(Collectors.toList());

		refreshChart(diseasesBurden);
		chart.setHeight(visibleDiseasesCount * 55, Unit.PIXELS);
	}

	private List<DiseaseBurdenDto> mockDataUp(List<DiseaseBurdenDto> data) {
		List<DiseaseBurdenDto> newData = new ArrayList<DiseaseBurdenDto>();

		Long diff = 6L;
		for (DiseaseBurdenDto diseaseBurden : data) {
			Long caseCount = 0L;
			Long previousCaseCount = 0L;

			if (diff >= 0)
				caseCount = diff;
			else
				previousCaseCount = Math.abs(diff);

			newData.add(new DiseaseBurdenDto(diseaseBurden.getDisease(), caseCount, previousCaseCount,
					diseaseBurden.getEventCount(), diseaseBurden.getOutbreakDistrictCount(),
					diseaseBurden.getCaseDeathCount()));

			diff -= 2;
		}

		return newData;
	}

	private void refreshChart(List<DiseaseBurdenDto> data) {
		StringBuilder hcjs = new StringBuilder();
		hcjs.append("var options = {" + "chart:{ " + " type: 'bar', " + " backgroundColor: 'transparent' " + "},"
				+ "credits:{ enabled: false }," + "exporting:{ " + " enabled: false,"
				+ " buttons:{ contextButton:{ theme:{ fill: 'transparent' } } }" + "}," + "title:{ text: '' },");

		hcjs.append("xAxis: { categories: [");
		for (DiseaseBurdenDto s : data) {
			hcjs.append("'" + s.getDisease().toString() + "', ");
		}
		hcjs.append("]},");

		hcjs.append("yAxis: { title: { text: 'Difference' }, allowDecimals: false, " + "stackLabels: { enabled: true, "
				+ "style: {fontWeight: 'normal', textOutline: '0', gridLineColor: '#000000', color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray' } } },"
				+ "legend: { verticalAlign: 'middle', backgroundColor: 'transparent', align: 'right', layout: 'vertical', "
				+ "borderWidth: 0, shadow: false, margin: 30, padding: 0 },"
				+ "tooltip: { headerFormat: '<b>{point.x}</b><br/>', pointFormat: '{series.name}: {point.y}<br/>Total: {point.stackTotal}'},"
				+ "plotOptions: { column: { borderWidth: 0, stacking: 'normal', groupPadding: 0, pointPadding: 0, dataLabels: {"
				+ "enabled: true, formatter: function() { if (this.y > 0) return this.y; },"
				+ "color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white' } }, series: { pointWidth: 20 } },");

		/*
		 * split data into two groups (positive and negative) positive group will be
		 * padded with zeros of negative group and negative group will also be padded
		 * with zeros of positive group
		 * 
		 */
		List<Long> positive_values = data.stream().filter((d) -> d.getCasesDifference() >= 0)
				.map((d) -> d.getCasesDifference()).collect(Collectors.toList());
		List<Long> negative_values = data.stream().filter((d) -> d.getCasesDifference() < 0)
				.map((d) -> d.getCasesDifference()).collect(Collectors.toList());

		hcjs.append("series: [");

		hcjs.append("{ name: 'More than last year', color: '#FF4500', data: [");
		hcjs.append(Stream.concat(positive_values.stream(), negative_values.stream().map((v) -> 0)) // pad with negative
																									// group
				.map((d) -> d.toString()).reduce((fullText, nextText) -> fullText + ", " + nextText).get());
		hcjs.append("]},");

		hcjs.append("{ name: 'Less than last year', color: '#32CD32', data: [");
		hcjs.append(Stream.concat(positive_values.stream().map((v) -> 0), negative_values.stream()) // pad with positive
																									// group
				.map((d) -> d.toString()).reduce((fullText, nextText) -> fullText + ", " + nextText).get());
		hcjs.append("]},");

		hcjs.append("],"); // series: []

		hcjs.append("}"); // options: {}

		chart.setHcjs(hcjs.toString());
	}

}
