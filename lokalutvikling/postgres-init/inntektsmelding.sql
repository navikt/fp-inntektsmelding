CREATE DATABASE fpinntektsmelding;
CREATE USER fpinntektsmelding WITH PASSWORD 'fpinntektsmelding';
GRANT ALL ON DATABASE fpinntektsmelding TO fpinntektsmelding;
ALTER DATABASE fpinntektsmelding SET timezone TO 'Europe/Oslo';
ALTER DATABASE fpinntektsmelding OWNER TO fpinntektsmelding;
