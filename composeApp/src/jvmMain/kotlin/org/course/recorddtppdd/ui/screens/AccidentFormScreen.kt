package org.course.recorddtppdd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.course.recorddtppdd.viewmodel.*

private val GUILTY_OPTIONS = listOf("A", "B", "не определён")

@Composable
fun AccidentFormScreen(
    vm: AccidentFormViewModel,
    officerId: Int,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Оформление ДТП",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        StepIndicator(current = vm.currentStep, total = vm.totalSteps)
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (vm.currentStep) {
                0 -> Step1GeneralInfo(vm)
                1 -> Step2VehicleData(vm)
                2 -> Step3DriverData(vm)
                3 -> Step4Damages(vm)
                4 -> Step5AccidentType(vm)
                5 -> Step6Completion(vm)
            }
        }

        if (vm.validationError.isNotBlank()) {
            Text(vm.validationError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
        }
        if (vm.saveError.isNotBlank()) {
            Text(vm.saveError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (vm.currentStep > 0) {
                OutlinedButton(onClick = { vm.prevStep() }) { Text("Назад") }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (vm.currentStep < vm.totalSteps - 1) {
                Button(
                    onClick = { vm.tryNextStep() },
                    enabled = vm.canGoNext()
                ) { Text("Далее") }
            } else {
                Button(
                    onClick = { vm.save(officerId, onDone) },
                    enabled = vm.canSave()
                ) {
                    if (vm.isSaving) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    else Text("Сохранить")
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    val stepNames = listOf(
        "Общее", "Данные ТС", "Водитель", "Повреждения", "Тип ДТП", "Завершение"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stepNames.take(total).forEachIndexed { i, name ->
            val active = i == current
            val done = i < current
            FilterChip(
                selected = active || done,
                onClick = {},
                label = { Text("${i + 1}. $name", fontSize = 11.sp) },
                enabled = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Step1GeneralInfo(vm: AccidentFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Место ДТП")
        FormField("Улица", vm.locationStreet) { vm.locationStreet = it }
        FormField("Номер дома / км", vm.locationBuilding) { vm.locationBuilding = it }

        SectionTitle("Дата и время")
        FormField("Дата (гггг-мм-дд)", vm.accidentDate) { vm.accidentDate = it }
        FormField("Время (чч:мм)", vm.accidentTime) { vm.accidentTime = it }

        SectionTitle("Свидетели")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = vm.noWitnesses, onCheckedChange = { vm.noWitnesses = it })
            Text("Свидетели отсутствуют")
        }
        if (!vm.noWitnesses) {
            FormField("ФИО свидетеля", vm.witnessesName) { vm.witnessesName = it }
            FormField("Адрес свидетеля", vm.witnessesAddress) { vm.witnessesAddress = it }
        }
    }
}

@Composable
private fun Step2VehicleData(vm: AccidentFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("ТС участника A")
        VehicleFields(vm.vehicleA) { vm.vehicleA = it }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("ТС участника B")
        VehicleFields(vm.vehicleB) { vm.vehicleB = it }
    }
}

@Composable
private fun VehicleFields(data: VehicleFormData, onUpdate: (VehicleFormData) -> Unit) {
    FormField("Марка", data.make) { onUpdate(data.copy(make = it)) }
    FormField("Модель", data.model) { onUpdate(data.copy(model = it)) }
    FormField("VIN", data.vin) { onUpdate(data.copy(vin = it)) }
    FormField("Госномер", data.numberPlate) { onUpdate(data.copy(numberPlate = it)) }
    FormField("СТС (серия, номер)", data.sts) { onUpdate(data.copy(sts = it)) }
    FormField("Собственник (ФИО)", data.ownerName) { onUpdate(data.copy(ownerName = it)) }
    FormField("Адрес собственника", data.ownerAddress) { onUpdate(data.copy(ownerAddress = it)) }
}

@Composable
private fun Step3DriverData(vm: AccidentFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Водитель A")
        DriverFields(vm.driverA) { vm.driverA = it }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Водитель B")
        DriverFields(vm.driverB) { vm.driverB = it }
    }
}

@Composable
private fun DriverFields(data: DriverFormData, onUpdate: (DriverFormData) -> Unit) {
    FormField("ФИО", data.fullName) { onUpdate(data.copy(fullName = it)) }
    FormField("Дата рождения (гггг-мм-дд)", data.birthdate) { onUpdate(data.copy(birthdate = it)) }
    FormField("Адрес", data.address) { onUpdate(data.copy(address = it)) }
    FormField("Телефон", data.phone) { onUpdate(data.copy(phone = it)) }
    Text(
        "Водительское удостоверение",
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = data.licSeries, onValueChange = { onUpdate(data.copy(licSeries = it)) },
            label = { Text("Серия") }, modifier = Modifier.weight(1f), singleLine = true
        )
        OutlinedTextField(
            value = data.licNumber, onValueChange = { onUpdate(data.copy(licNumber = it)) },
            label = { Text("Номер") }, modifier = Modifier.weight(1f), singleLine = true
        )
        OutlinedTextField(
            value = data.licCategory, onValueChange = { onUpdate(data.copy(licCategory = it)) },
            label = { Text("Категория") }, modifier = Modifier.weight(1f), singleLine = true
        )
    }
    FormField("Дата выдачи ВУ (гггг-мм-дд)", data.licIssueDate) { onUpdate(data.copy(licIssueDate = it)) }
    Text("Страховой полис", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
    FormField("Наименование страховой", data.insCompany) { onUpdate(data.copy(insCompany = it)) }
    FormField("Номер полиса", data.insPolicy) { onUpdate(data.copy(insPolicy = it)) }
    FormField("Дата окончания полиса", data.insExpiry) { onUpdate(data.copy(insExpiry = it)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = data.hasHull, onCheckedChange = { onUpdate(data.copy(hasHull = it)) })
        Text("КАСКО")
    }
}

@Composable
private fun Step4Damages(vm: AccidentFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Повреждения ТС A")
        OutlinedTextField(
            value = vm.damagesA, onValueChange = { vm.damagesA = it },
            label = { Text("Перечень повреждений") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        OutlinedTextField(
            value = vm.notesA, onValueChange = { vm.notesA = it },
            label = { Text("Замечания") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Повреждения ТС B")
        OutlinedTextField(
            value = vm.damagesB, onValueChange = { vm.damagesB = it },
            label = { Text("Перечень повреждений") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        OutlinedTextField(
            value = vm.notesB, onValueChange = { vm.notesB = it },
            label = { Text("Замечания") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
    }
}

@Composable
private fun Step5AccidentType(vm: AccidentFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Обстоятельства / тип ДТП")
        if (vm.accidentTypes.isEmpty()) {
            Text("Справочник типов ДТП пуст или не загружен.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { vm.loadAccidentTypes() }) { Text("Повторить загрузку") }
        } else {
            vm.accidentTypes.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = vm.selectedAccidentTypeId == type.id,
                        onClick = { vm.selectedAccidentTypeId = type.id }
                    )
                    Column {
                        Text(type.name, fontSize = 14.sp)
                        if (!type.description.isNullOrBlank()) {
                            Text(type.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step6Completion(vm: AccidentFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Завершение оформления")
        OutlinedTextField(
            value = vm.accidentDescription,
            onValueChange = { vm.accidentDescription = it },
            label = { Text("Текстовое описание ДТП") },
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )
        SectionTitle("Определение виновного")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GUILTY_OPTIONS.forEach { opt ->
                FilterChip(
                    selected = vm.guiltySide == opt,
                    onClick = { vm.guiltySide = opt },
                    label = { Text(opt) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Сводка:", fontWeight = FontWeight.SemiBold)
                Text("Место: ${vm.locationStreet} ${vm.locationBuilding}", fontSize = 13.sp)
                Text("Дата/время: ${vm.accidentDate} ${vm.accidentTime}", fontSize = 13.sp)
                Text("Виновный: ${vm.guiltySide}", fontSize = 13.sp)
                Text("Тип ДТП ID: ${vm.selectedAccidentTypeId ?: "-"}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
