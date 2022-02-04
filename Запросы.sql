INSERT  INTO users ("username","email","first_name","last_name","password")
VALUES ('Артём','maa.ofp@yande.com','Артём','Меньшиков','$2a$09$r4wIopqdY.ZoOrQLxPA2RenuAIDQysbdHE.PdJJyL7ujFpvN78p.i');

SELECT now() at time zone 'utc-3';
SELECT CURRENT_TIMESTAMP at time zone 'utc-3';
SELECT CURRENT_TIMESTAMP at time zone 'MSK';


CREATE TYPE "TaskStatus" AS ENUM ('В_ОЧЕРЕДИ', 'ВЫПОЛНЕНИЕ', 'ОШИБКА_ВЫПОЛНЕНИЯ', 'ОШИБКА_КОМПИЛЯЦИИ', 'ЗАВЕРШЕНА');
CREATE TYPE "UserStatus" AS ENUM ('ДЕЙСТВУЮЩИЙ', 'НЕ_ДЕЙСТВУЮЩИЙ', 'УДАЛЁН');
 ALTER TABLE "Task" ALTER COLUMN "Status" TYPE "TaskStatus" USING "Status"::"TaskStatus";
SELECT "TaskStatus" from public."Tasks"

select * from "Task";

SELECT count(*) FROM task t WHERE t.user_id = 1 AND '2022-02-01 03:58:21.882945'<t.created AND t.created <'2022-02-01 16:21:40.333000';

SELECT t FROM task t WHERE t.user_id = 15;

UPDATE task set data_file_name = 'asdf', source_file_name='ihpa' WHERE id = 15444;

SELECT * FROM task t WHERE t.id = 15;

UPDATE task t SET source_file_name = '4654' WHERE t.id = 15 AND t.user_id = 1;


create function task_status_update_trigger_func() returns trigger
    language plpgsql
as
$$
declare
--         old_val TEXT;
        begin
            if (tg_op = 'update') then
                if( (not new.source_file_name=null) && (not new.data_file_name!=null)) then
                    UPDATE task t SET status = 'ОЖИДАНИЕ_ЗАПУСКА' WHERE t.id = new.id;
                elseif (new.source_file_name=null&& (not new.data_file_name=null)) then
                    UPDATE task t SET status = 'ОЖИДАНИЕ_ИСХОДНИКОВ' WHERE t.id = new.id;
                elseif ((not new.source_file_name!=null)&&new.data_file_name=null) then
                    UPDATE task t SET status = 'ОЖИДАНИЕ_ДАННЫХ' WHERE t.id = new.id;
                end if;
            end if;
            return new;
        end;
$$;

create or replace function test_trigger_func() returns trigger
    language plpgsql
as
$$
declare
--         old_val TEXT;
        begin
            INSERT INTO task (status) VALUES ('ОЖИДАНИЕ_ЗАПУСКА');
--             UPDATE task t SET status = 'ОЖИДАНИЕ_ЗАПУСКА' WHERE t.id = 79;
            return new;
        end;
$$;


alter function task_status_update_trigger_func() owner to postgres;


create trigger task_status_update_trigger
    after update
    on task
execute procedure task_status_update_trigger_func();

create trigger test_trigger
    after update
    on task
execute procedure test_trigger_func();


UPDATE task t SET name = 'Задача45' WHERE t.id = 79;

SELECT * FROM pg_trigger;