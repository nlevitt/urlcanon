#!/usr/bin/env python
'''
setup.py - setuptools installation configuration for urlcanon

Copyright (C) 2016 Internet Archive

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import setuptools

dependencies = []
try:
    import ipaddress
except ImportError:
    dependencies.append('py2-ipaddress')

setuptools.setup(
        name='urlcanon',
        version='0.1.dev1',
        packages=['urlcanon'],
        install_requires=dependencies,
        tests_require=['pytest'])

