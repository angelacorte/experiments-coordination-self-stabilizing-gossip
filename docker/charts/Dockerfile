FROM python:3.14.3
RUN mkdir /experiment
WORKDIR /experiment
COPY .python-version ./
COPY plot.py ./
COPY requirements.txt ./
RUN pip install --upgrade pip
RUN pip install -r requirements.txt
CMD python plot.py