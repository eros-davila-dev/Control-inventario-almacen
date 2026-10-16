-- =====================================================================
-- V3: maestros iniciales del almacen de CID.
-- Unidades de medida, categorias, red de almacenes y ubicaciones,
-- proveedores y catalogo de insumos y medicamentos de hemodialisis con
-- sus parametros de reposicion y datos sanitarios.
-- Los saldos NO se cargan aqui: el inventario inicial se registra desde
-- el sistema con un ajuste de motivo SALDO_INICIAL, para que exista
-- kardex y trazabilidad desde el primer movimiento.
-- =====================================================================

set search_path to sgai;

-- ---------------------------------------------------------------------
-- Unidades de medida (las unidades base tienen um_base_id nulo y factor 1)
-- ---------------------------------------------------------------------
insert into sgai.unidad_medida (codigo, nombre, simbolo, factor_a_base, activo, created_at, created_by)
values
    ('UND', 'Unidad', 'und', 1, true, now(), 'migracion'),
    ('LT', 'Litro', 'L', 1, true, now(), 'migracion'),
    ('KG', 'Kilogramo', 'kg', 1, true, now(), 'migracion'),
    ('PAR', 'Par', 'par', 1, true, now(), 'migracion'),
    ('FRC', 'Frasco', 'frc', 1, true, now(), 'migracion'),
    ('AMP', 'Ampolla', 'amp', 1, true, now(), 'migracion'),
    ('SET', 'Set o kit', 'set', 1, true, now(), 'migracion'),
    ('CJ20', 'Caja por 20 unidades', 'cja', 20, true, now(), 'migracion'),
    ('CJ50', 'Caja por 50 unidades', 'cja', 50, true, now(), 'migracion'),
    ('CJ100', 'Caja por 100 unidades', 'cja', 100, true, now(), 'migracion'),
    ('GAL', 'Galon (3.785 L)', 'gal', 3.785, true, now(), 'migracion'),
    ('ML', 'Mililitro', 'mL', 0.001, true, now(), 'migracion');

-- Conversion de las unidades derivadas hacia su unidad base
update sgai.unidad_medida set um_base_id = (select id from sgai.unidad_medida where codigo = 'UND')
 where codigo in ('CJ20', 'CJ50', 'CJ100');
update sgai.unidad_medida set um_base_id = (select id from sgai.unidad_medida where codigo = 'LT')
 where codigo in ('GAL', 'ML');

-- ---------------------------------------------------------------------
-- Categorias del catalogo (dos niveles)
-- ---------------------------------------------------------------------
insert into sgai.categoria_producto (codigo, nombre, descripcion, requiere_cadena_frio, activo,
                                     created_at, created_by)
values
    ('DIAL', 'Insumos de hemodialisis', 'Material de uso directo en la sesion de hemodialisis', false, true, now(), 'migracion'),
    ('MED', 'Medicamentos', 'Medicamentos de soporte al tratamiento dialitico', false, true, now(), 'migracion'),
    ('MATM', 'Material medico', 'Material medico general y descartable', false, true, now(), 'migracion'),
    ('EPP', 'Equipos de proteccion personal', 'Proteccion del personal asistencial', false, true, now(), 'migracion'),
    ('LIMP', 'Limpieza y desinfeccion', 'Desinfeccion de maquinas, superficies y planta de agua', false, true, now(), 'migracion'),
    ('REPM', 'Repuestos de maquinas', 'Repuestos de maquinas de hemodialisis y planta de tratamiento de agua', false, true, now(), 'migracion'),
    ('LAB', 'Reactivos de laboratorio', 'Reactivos de control de calidad del agua y laboratorio', false, true, now(), 'migracion'),
    ('OFI', 'Utiles de oficina', 'Material administrativo', false, true, now(), 'migracion');

insert into sgai.categoria_producto (codigo, nombre, descripcion, categoria_padre_id,
                                     requiere_cadena_frio, activo, created_at, created_by)
select d.codigo, d.nombre, d.descripcion, p.id, d.frio, true, now(), 'migracion'
from (values
    ('DIAL-FIL', 'Dializadores', 'Filtros de alto y bajo flujo', 'DIAL', false),
    ('DIAL-LIN', 'Lineas y agujas', 'Lineas arteriovenosas, agujas de fistula y conectores', 'DIAL', false),
    ('DIAL-CON', 'Concentrados y soluciones', 'Concentrado acido, bicarbonato y soluciones', 'DIAL', false),
    ('DIAL-CAT', 'Cateteres y accesos', 'Cateteres venosos centrales y kits de curacion', 'DIAL', false),
    ('MED-REF', 'Medicamentos refrigerados', 'Cadena de frio 2-8 C', 'MED', true),
    ('MED-AMB', 'Medicamentos a temperatura ambiente', 'Conservacion 15-30 C', 'MED', false)
) as d(codigo, nombre, descripcion, padre, frio)
join sgai.categoria_producto p on p.codigo = d.padre;

-- ---------------------------------------------------------------------
-- Red de almacenes (sede central San Isidro y sede San Juan de Lurigancho)
-- ---------------------------------------------------------------------
insert into sgai.almacen (codigo, nombre, sede, direccion, tipo_almacen, telefono,
                          permite_negativo, activo, created_at, created_by)
values
    ('ALM-CEN', 'Almacen Central', 'San Isidro', 'Cal. Coronel Andres Reyes 437 Int. 1501 - San Isidro', 'CENTRAL', '942111005', false, true, now(), 'migracion'),
    ('ALM-FRI', 'Camara de Frio Central', 'San Isidro', 'Cal. Coronel Andres Reyes 437 Int. 1501 - San Isidro', 'FRIO', '942111005', false, true, now(), 'migracion'),
    ('ALM-CUA-SI', 'Cuarentena San Isidro', 'San Isidro', 'Cal. Coronel Andres Reyes 437 Int. 1501 - San Isidro', 'CUARENTENA', '942111005', false, true, now(), 'migracion'),
    ('ALM-REC', 'Rechazados y bajas', 'San Isidro', 'Cal. Coronel Andres Reyes 437 Int. 1501 - San Isidro', 'RECHAZADOS', '942111005', false, true, now(), 'migracion'),
    ('ALM-SJL', 'Almacen Unidad de Hemodialisis SJL', 'San Juan de Lurigancho', 'Av. Proceres de la Independencia - SJL', 'SEDE', '942111013', false, true, now(), 'migracion'),
    ('ALM-CUA-SJL', 'Cuarentena SJL', 'San Juan de Lurigancho', 'Av. Proceres de la Independencia - SJL', 'CUARENTENA', '942111013', false, true, now(), 'migracion'),
    ('ALM-FAR-SJL', 'Farmacia SJL', 'San Juan de Lurigancho', 'Av. Proceres de la Independencia - SJL', 'FARMACIA', '942111014', false, true, now(), 'migracion');

-- Ubicaciones del almacen central y de la sede
insert into sgai.ubicacion (almacen_id, codigo, descripcion, zona, rack, nivel, tipo_ubicacion,
                            activo, created_at, created_by)
select a.id, d.codigo, d.descripcion, d.zona, d.rack, d.nivel, d.tipo, true, now(), 'migracion'
from (values
    ('ALM-CEN', 'A-01-01', 'Dializadores alto flujo', 'A', '01', '01', 'PICKING'),
    ('ALM-CEN', 'A-01-02', 'Dializadores bajo flujo', 'A', '01', '02', 'PICKING'),
    ('ALM-CEN', 'A-02-01', 'Lineas arteriovenosas', 'A', '02', '01', 'PICKING'),
    ('ALM-CEN', 'A-02-02', 'Agujas de fistula', 'A', '02', '02', 'PICKING'),
    ('ALM-CEN', 'B-01-01', 'Concentrado acido', 'B', '01', '01', 'RESERVA'),
    ('ALM-CEN', 'B-01-02', 'Bicarbonato', 'B', '01', '02', 'RESERVA'),
    ('ALM-CEN', 'C-01-01', 'Material medico descartable', 'C', '01', '01', 'PICKING'),
    ('ALM-CEN', 'C-02-01', 'EPP', 'C', '02', '01', 'PICKING'),
    ('ALM-CEN', 'D-01-01', 'Limpieza y desinfeccion', 'D', '01', '01', 'RESERVA'),
    ('ALM-CEN', 'E-01-01', 'Repuestos de maquinas', 'E', '01', '01', 'RESERVA'),
    ('ALM-FRI', 'F-01-01', 'Refrigerado 2-8 C estante 1', 'F', '01', '01', 'FRIO'),
    ('ALM-FRI', 'F-01-02', 'Refrigerado 2-8 C estante 2', 'F', '01', '02', 'FRIO'),
    ('ALM-CUA-SI', 'Q-01-01', 'Cuarentena pendiente de liberacion', 'Q', '01', '01', 'CUARENTENA'),
    ('ALM-SJL', 'S-01-01', 'Insumos de sesion - turno manana', 'S', '01', '01', 'PICKING'),
    ('ALM-SJL', 'S-01-02', 'Insumos de sesion - turno tarde', 'S', '01', '02', 'PICKING'),
    ('ALM-SJL', 'S-02-01', 'Concentrados y soluciones', 'S', '02', '01', 'RESERVA'),
    ('ALM-CUA-SJL', 'Q-02-01', 'Cuarentena SJL', 'Q', '02', '01', 'CUARENTENA'),
    ('ALM-FAR-SJL', 'G-01-01', 'Medicamentos de sesion', 'G', '01', '01', 'PICKING')
) as d(almacen, codigo, descripcion, zona, rack, nivel, tipo)
join sgai.almacen a on a.codigo = d.almacen;

-- Alcance operativo de los almaceneros y de enfermeria (HU-14)
insert into sgai.usuario_almacen (usuario_id, almacen_id)
select u.id, a.id from sgai.usuario u join sgai.almacen a on a.codigo in ('ALM-CEN', 'ALM-FRI', 'ALM-CUA-SI', 'ALM-REC')
 where u.username = 'almacenero.central';
insert into sgai.usuario_almacen (usuario_id, almacen_id)
select u.id, a.id from sgai.usuario u join sgai.almacen a on a.codigo in ('ALM-SJL', 'ALM-CUA-SJL', 'ALM-FAR-SJL')
 where u.username = 'almacenero.sjl';
insert into sgai.usuario_almacen (usuario_id, almacen_id)
select u.id, a.id from sgai.usuario u join sgai.almacen a on a.codigo = 'ALM-SJL'
 where u.username = 'enfermeria.sjl';

-- ---------------------------------------------------------------------
-- Proveedores
-- ---------------------------------------------------------------------
insert into sgai.proveedor (ruc, razon_social, nombre_comercial, contacto, telefono, email,
                            direccion, condicion_pago, lead_time_dias, activo, created_at, created_by)
values
    ('20100334455', 'DISTRIBUIDORA DE INSUMOS RENALES S.A.C.', 'DIRSAC', 'Mario Paredes', '014567890', 'ventas@dirsac.com.pe', 'Av. Argentina 1520 - Lima', 'CREDITO 30 DIAS', 10, true, now(), 'migracion'),
    ('20522677881', 'MEDIFLUIDOS DEL PERU S.A.', 'MEDIFLUIDOS', 'Carmen Yupanqui', '014112233', 'contacto@medifluidos.pe', 'Av. Separadora Industrial 2255 - Ate', 'CREDITO 45 DIAS', 14, true, now(), 'migracion'),
    ('20601234562', 'FARMA RENAL IMPORTACIONES S.A.C.', 'FARMA RENAL', 'Diego Castillo', '016543210', 'importaciones@farmarenal.pe', 'Jr. Huallaga 220 - Lima', 'CREDITO 30 DIAS', 21, true, now(), 'migracion'),
    ('20498877665', 'SUMINISTROS CLINICOS ANDINOS E.I.R.L.', 'SUCLAN', 'Elena Rojas', '013344556', 'ventas@suclan.pe', 'Av. Tomas Marsano 1180 - Surquillo', 'CONTADO', 5, true, now(), 'migracion'),
    ('20345678913', 'TECNOSERVICIO DE EQUIPOS MEDICOS S.A.C.', 'TECNOMED', 'Raul Salazar', '017788990', 'servicio@tecnomed.pe', 'Av. Colonial 3540 - Callao', 'CREDITO 60 DIAS', 30, true, now(), 'migracion');

-- ---------------------------------------------------------------------
-- Catalogo de productos
-- Los insumos y medicamentos llevan control de lote y vencimiento; los
-- parametros de reposicion se calcularon sobre un promedio de 1 800
-- sesiones de hemodialisis al mes entre las dos sedes.
-- ---------------------------------------------------------------------
insert into sgai.producto (codigo, nombre, descripcion, categoria_id, unidad_medida_id, tipo_producto,
                           presentacion, marca, registro_sanitario, vence_registro_sanitario,
                           condicion_almacenamiento, controla_lote, controla_vencimiento,
                           vida_util_minima_dias, clasificacion_abc, stock_minimo, punto_reorden,
                           stock_maximo, dias_cobertura, costo_promedio, critico, activo,
                           created_at, created_by)
select d.codigo, d.nombre, d.descripcion, c.id, u.id, d.tipo, d.presentacion, d.marca, d.rs,
       d.vence_rs, d.condicion, d.lote, d.venc, d.vida, d.abc, d.minimo, d.reorden, d.maximo,
       d.cobertura, 0, d.critico, true, now(), 'migracion'
from (values
    -- Dializadores
    ('DIA-HF18', 'Dializador de alto flujo 1.8 m2', 'Membrana de polisulfona, superficie 1.8 m2, uso unico', 'DIAL-FIL', 'UND', 'INSUMO_DIALISIS', 'Unidad esteril', 'RenalTech', 'DM-2024-18745', date '2029-03-31', 'AMBIENTE', true, true, 365, 'A', 600, 900, 2400, 30, true),
    ('DIA-HF15', 'Dializador de alto flujo 1.5 m2', 'Membrana de polisulfona, superficie 1.5 m2, uso unico', 'DIAL-FIL', 'UND', 'INSUMO_DIALISIS', 'Unidad esteril', 'RenalTech', 'DM-2024-18746', date '2029-03-31', 'AMBIENTE', true, true, 365, 'A', 400, 600, 1600, 30, true),
    ('DIA-LF13', 'Dializador de bajo flujo 1.3 m2', 'Membrana de polisulfona, superficie 1.3 m2, uso unico', 'DIAL-FIL', 'UND', 'INSUMO_DIALISIS', 'Unidad esteril', 'RenalTech', 'DM-2023-15502', date '2028-11-30', 'AMBIENTE', true, true, 365, 'B', 150, 250, 700, 30, false),
    -- Lineas y agujas
    ('LIN-AV01', 'Linea arteriovenosa para hemodialisis', 'Set arterial y venoso con camaras y conectores luer lock', 'DIAL-LIN', 'SET', 'INSUMO_DIALISIS', 'Set esteril', 'RenalTech', 'DM-2024-18801', date '2029-03-31', 'AMBIENTE', true, true, 365, 'A', 600, 900, 2400, 30, true),
    ('AGU-FI16', 'Aguja de fistula 16G x 1"', 'Aguja de fistula arteriovenosa calibre 16G con aleta', 'DIAL-LIN', 'UND', 'INSUMO_DIALISIS', 'Unidad esteril', 'VenoPunct', 'DM-2024-19020', date '2028-08-31', 'AMBIENTE', true, true, 270, 'A', 1200, 1800, 4800, 30, true),
    ('AGU-FI17', 'Aguja de fistula 17G x 1"', 'Aguja de fistula arteriovenosa calibre 17G con aleta', 'DIAL-LIN', 'UND', 'INSUMO_DIALISIS', 'Unidad esteril', 'VenoPunct', 'DM-2024-19021', date '2028-08-31', 'AMBIENTE', true, true, 270, 'B', 400, 600, 1600, 30, false),
    ('CON-TRA1', 'Conector de transicion para cateter', 'Conector esteril de transicion para cateter venoso central', 'DIAL-LIN', 'UND', 'INSUMO_DIALISIS', 'Unidad esteril', 'VenoPunct', 'DM-2023-15880', date '2028-05-31', 'AMBIENTE', true, true, 180, 'C', 60, 100, 300, 45, false),
    -- Concentrados y soluciones
    ('CON-ACI1', 'Concentrado acido para hemodialisis', 'Concentrado acido para bano de dialisis, dilucion 1:34', 'DIAL-CON', 'GAL', 'INSUMO_DIALISIS', 'Galon de 3.785 L', 'DialSol', 'NSO-2024-04410', date '2029-01-31', 'AMBIENTE', true, true, 180, 'A', 300, 450, 1200, 30, true),
    ('CON-BIC1', 'Bicarbonato de sodio en cartucho 650 g', 'Cartucho de bicarbonato de sodio para maquina de hemodialisis', 'DIAL-CON', 'UND', 'INSUMO_DIALISIS', 'Cartucho 650 g', 'DialSol', 'NSO-2024-04411', date '2029-01-31', 'AMBIENTE', true, true, 180, 'A', 600, 900, 2400, 30, true),
    ('SOL-NAC9', 'Cloruro de sodio 0.9% 1000 mL', 'Solucion salina fisiologica en bolsa de 1000 mL', 'DIAL-CON', 'UND', 'MEDICAMENTO', 'Bolsa 1000 mL', 'Sueros Peru', 'EE-04578', date '2028-12-31', 'AMBIENTE', true, true, 270, 'A', 700, 1000, 2600, 30, true),
    ('SOL-CIT4', 'Citrato de sodio 4% 10 mL', 'Solucion de sellado de cateter', 'DIAL-CON', 'AMP', 'MEDICAMENTO', 'Ampolla 10 mL', 'Sueros Peru', 'EE-04602', date '2028-10-31', 'AMBIENTE', true, true, 180, 'B', 200, 300, 800, 30, false),
    -- Cateteres y accesos
    ('CAT-DL12', 'Cateter venoso central doble lumen 12 Fr', 'Cateter temporal doble lumen para hemodialisis', 'DIAL-CAT', 'SET', 'MATERIAL_MEDICO', 'Kit esteril', 'VascuLine', 'DM-2023-16110', date '2028-07-31', 'AMBIENTE', true, true, 365, 'B', 20, 35, 90, 60, true),
    ('KIT-CUR1', 'Kit de curacion de cateter', 'Kit esteril de curacion con gasas, campo y antiseptico', 'DIAL-CAT', 'SET', 'MATERIAL_MEDICO', 'Kit esteril', 'SUCLAN', 'DM-2024-17003', date '2029-02-28', 'AMBIENTE', true, true, 180, 'B', 150, 250, 700, 30, false),
    -- Medicamentos refrigerados
    ('MED-EPO4', 'Eritropoyetina humana recombinante 4000 UI', 'Jeringa precargada 4000 UI, conservar 2-8 C', 'MED-REF', 'FRC', 'MEDICAMENTO', 'Jeringa precargada', 'HemoPharma', 'EE-02110', date '2028-09-30', 'REFRIGERADO_2_8', true, true, 180, 'A', 200, 320, 900, 30, true),
    ('MED-EPO2', 'Eritropoyetina humana recombinante 2000 UI', 'Jeringa precargada 2000 UI, conservar 2-8 C', 'MED-REF', 'FRC', 'MEDICAMENTO', 'Jeringa precargada', 'HemoPharma', 'EE-02111', date '2028-09-30', 'REFRIGERADO_2_8', true, true, 180, 'B', 80, 140, 400, 30, false),
    ('MED-CALC', 'Calcitriol 1 mcg/mL ampolla', 'Ampolla de calcitriol para administracion endovenosa', 'MED-REF', 'AMP', 'MEDICAMENTO', 'Ampolla 1 mL', 'HemoPharma', 'EE-02230', date '2028-06-30', 'REFRIGERADO_2_8', true, true, 180, 'B', 100, 160, 450, 30, false),
    -- Medicamentos a temperatura ambiente
    ('MED-HEP5', 'Heparina sodica 5000 UI/mL frasco 5 mL', 'Anticoagulacion del circuito extracorporeo', 'MED-AMB', 'FRC', 'MEDICAMENTO', 'Frasco 5 mL', 'Farmalab', 'EE-01980', date '2029-04-30', 'AMBIENTE', true, true, 270, 'A', 250, 380, 1000, 30, true),
    ('MED-FESA', 'Hierro sacarato 100 mg/5 mL', 'Ampolla de hierro sacarato endovenoso', 'MED-AMB', 'AMP', 'MEDICAMENTO', 'Ampolla 5 mL', 'Farmalab', 'EE-02001', date '2029-04-30', 'AMBIENTE', true, true, 270, 'A', 300, 450, 1200, 30, true),
    ('MED-LIDO', 'Lidocaina 2% 20 mL', 'Anestesico local para puncion de fistula', 'MED-AMB', 'FRC', 'MEDICAMENTO', 'Frasco 20 mL', 'Farmalab', 'EE-01760', date '2028-12-31', 'AMBIENTE', true, true, 180, 'C', 40, 70, 200, 45, false),
    -- Material medico
    ('MAT-GAS1', 'Gasa esteril 10 x 10 cm', 'Gasa esteril de 8 capas, sobre individual', 'MATM', 'UND', 'MATERIAL_MEDICO', 'Sobre x 1', 'SUCLAN', 'DM-2024-17200', date '2029-05-31', 'AMBIENTE', true, true, 365, 'B', 2000, 3000, 8000, 30, false),
    ('MAT-JER5', 'Jeringa descartable 5 mL', 'Jeringa de 5 mL con aguja 21G', 'MATM', 'UND', 'MATERIAL_MEDICO', 'Unidad esteril', 'SUCLAN', 'DM-2024-17210', date '2029-05-31', 'AMBIENTE', true, true, 365, 'B', 1500, 2200, 6000, 30, false),
    ('MAT-JER10', 'Jeringa descartable 10 mL', 'Jeringa de 10 mL con aguja 21G', 'MATM', 'UND', 'MATERIAL_MEDICO', 'Unidad esteril', 'SUCLAN', 'DM-2024-17211', date '2029-05-31', 'AMBIENTE', true, true, 365, 'B', 1200, 1800, 5000, 30, false),
    ('MAT-ESPA', 'Esparadrapo hipoalergenico 2.5 cm x 5 m', 'Cinta adhesiva hipoalergenica', 'MATM', 'UND', 'MATERIAL_MEDICO', 'Rollo', 'SUCLAN', 'DM-2023-16500', date '2028-04-30', 'AMBIENTE', false, false, 90, 'C', 100, 160, 400, 60, false),
    -- EPP
    ('EPP-GUAN', 'Guantes de nitrilo talla M', 'Guantes de examen de nitrilo sin polvo', 'EPP', 'CJ100', 'EPP', 'Caja x 100', 'SafeCare', 'DM-2024-17500', date '2029-06-30', 'AMBIENTE', false, false, 90, 'A', 60, 90, 240, 30, true),
    ('EPP-MASC', 'Mascarilla quirurgica tres pliegues', 'Mascarilla quirurgica con elastico', 'EPP', 'CJ50', 'EPP', 'Caja x 50', 'SafeCare', 'DM-2024-17501', date '2029-06-30', 'AMBIENTE', false, false, 90, 'B', 80, 120, 320, 30, false),
    ('EPP-MAND', 'Mandilon descartable manga larga', 'Mandilon de polipropileno descartable', 'EPP', 'UND', 'EPP', 'Unidad', 'SafeCare', null, null, 'AMBIENTE', false, false, 90, 'B', 500, 800, 2000, 30, false),
    -- Limpieza y desinfeccion
    ('LIM-HIPO', 'Hipoclorito de sodio 5%', 'Desinfectante de superficies y circuito hidraulico', 'LIMP', 'GAL', 'LIMPIEZA', 'Galon', 'CleanLab', 'NSO-2023-09920', date '2028-03-31', 'AMBIENTE', true, true, 180, 'B', 40, 60, 160, 45, false),
    ('LIM-PERA', 'Acido peracetico para desinfeccion de maquinas', 'Desinfectante de circuito de hemodialisis', 'LIMP', 'GAL', 'LIMPIEZA', 'Galon', 'CleanLab', 'NSO-2023-09921', date '2028-03-31', 'PROTEGIDO_LUZ', true, true, 180, 'A', 50, 80, 200, 30, true),
    ('LIM-ALCO', 'Alcohol etilico 70 grados', 'Antiseptico de piel y superficies', 'LIMP', 'LT', 'LIMPIEZA', 'Botella 1 L', 'CleanLab', 'NSO-2024-10110', date '2029-01-31', 'AMBIENTE', true, true, 180, 'B', 80, 120, 320, 45, false),
    -- Repuestos y laboratorio
    ('REP-FILO', 'Filtro de osmosis inversa 20"', 'Filtro de la planta de tratamiento de agua', 'REPM', 'UND', 'REPUESTO_MAQUINA', 'Unidad', 'TECNOMED', null, null, 'AMBIENTE', false, false, 90, 'C', 8, 14, 40, 90, true),
    ('REP-TRPR', 'Transductor de presion para maquina de HD', 'Repuesto de transductor de presion arterial/venosa', 'REPM', 'UND', 'REPUESTO_MAQUINA', 'Unidad', 'TECNOMED', null, null, 'AMBIENTE', false, false, 90, 'C', 6, 10, 30, 90, false),
    ('LAB-DURE', 'Reactivo de dureza total del agua', 'Kit de medicion de dureza del agua tratada', 'LAB', 'SET', 'REACTIVO', 'Kit x 100 pruebas', 'AquaTest', 'NSO-2024-11200', date '2028-02-29', 'AMBIENTE', true, true, 120, 'C', 4, 8, 20, 90, false),
    ('LAB-CLOR', 'Reactivo de cloro residual', 'Kit de medicion de cloro residual del agua tratada', 'LAB', 'SET', 'REACTIVO', 'Kit x 100 pruebas', 'AquaTest', 'NSO-2024-11201', date '2028-02-29', 'AMBIENTE', true, true, 120, 'C', 4, 8, 20, 90, false)
) as d(codigo, nombre, descripcion, cat, um, tipo, presentacion, marca, rs, vence_rs, condicion,
       lote, venc, vida, abc, minimo, reorden, maximo, cobertura, critico)
join sgai.categoria_producto c on c.codigo = d.cat
join sgai.unidad_medida u on u.codigo = d.um;
