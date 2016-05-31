# ActiveFires

Приложение разработано с использованием NASA WorldWind для визуализации данных, PostgreSQL версии 9.5 в качестве базы данных с расширением PostGIS версии 2.2. Для импорта данных из файлов Shapefile в БД использовалась утилита ogr2ogr с драйвером PostgreSQL (импорт может занимать несколько минут в связи с особенностями работы утилиты ogr2ogr). Параметры базы данных: название — "postgres", имя пользователя — "postgres", пароль — "postgres", порт — 5432. 