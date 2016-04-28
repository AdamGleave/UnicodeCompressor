from celery import Celery

import benchmark.config as config

app = Celery('benchmark', include=['benchmark.tasks'])
app.conf.update(**config.CELERY)

if __name__ == '__main__':
  app.start()
