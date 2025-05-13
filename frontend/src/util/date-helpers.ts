/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
export type CalendarWeek = { week: number; isoYear: number; startDate: Date; }

export function getCalendarWeek(date: Date): CalendarWeek {
  const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  targetDate.setDate(targetDate.getDate() + 3 - (targetDate.getDay() || 7));
  const startOfTargetWeek = new Date(targetDate);
  startOfTargetWeek.setDate(targetDate.getDate() - 3);
  const isoYear = targetDate.getFullYear();
  const firstThursday = new Date(isoYear, 0, 4);
  const startOfFirstWeek = new Date(firstThursday);
  startOfFirstWeek.setDate(firstThursday.getDate() - (firstThursday.getDay() || 7));
  const week = Math.ceil(((+targetDate - +startOfFirstWeek) / 86400000 + 1) / 7);
  return { week, isoYear, startDate: startOfTargetWeek };
}

export function incrementCalendarWeek(cw: CalendarWeek, amount: number) {
  const {week, isoYear} = cw;
  const firstThursday = new Date(isoYear, 0, 4);
  const startOfFirstWeek = new Date(firstThursday);
  startOfFirstWeek.setDate(firstThursday.getDate() - (firstThursday.getDay() || 7) + 1);
  const currentWeekStart = new Date(startOfFirstWeek);
  currentWeekStart.setDate(startOfFirstWeek.getDate() + (week - 1) * 7);
  const nextWeekStart = new Date(currentWeekStart);
  nextWeekStart.setDate(currentWeekStart.getDate() + 7 * amount);
  const nextThursday = new Date(nextWeekStart);
  nextThursday.setDate(nextWeekStart.getDate() + 3 - (nextWeekStart.getDay() || 7));
  const newIsoYear = nextThursday.getFullYear();
  const newYearFirstThursday = new Date(newIsoYear, 0, 4);
  const newYearStartOfFirstWeek = new Date(newYearFirstThursday);
  newYearStartOfFirstWeek.setDate(newYearFirstThursday.getDate() - (newYearFirstThursday.getDay() || 7) + 1);
  const newWeekNumber = Math.ceil(((+nextWeekStart - +newYearStartOfFirstWeek) / 86400000 + 1) / 7);
  return { week: newWeekNumber, isoYear: newIsoYear, startDate: nextWeekStart };
}

export function incrementDate(date: Date, days: number) {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() + days);
  return newDate;
}
