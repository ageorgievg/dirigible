/*
 * Copyright (c) 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
var java = require('core/v3/java');

request = function(url, method, options) {
	var result = {};
	if (options) {
		result = java.call('org.eclipse.dirigible.api.v3.http.HttpClientFacade', method, [url, JSON.stringify(options)]);
	} else {
		result = java.call('org.eclipse.dirigible.api.v3.http.HttpClientFacade', method, [url, '{}']);
	}
	
	return JSON.parse(result);
};

exports.get = function(url, options) {
	url = buildUrl(url, options);
	return request(url, 'get', options);
};

exports.post = function(url, options) {
	return request(url, 'post', options);
};

exports.put = function(url, options) {
	return request(url, 'put', options);
};

exports.patch = function(url, options) {
	return request(url, 'patch', options);
};

exports.delete = function(url, options) {
	url = buildUrl(url, options);
	return request(url, 'delete', options);
};

exports.head = function(url, options) {
	return request(url, 'head', options);
};

exports.trace = function(url, options) {
	return request(url, 'trace', options);
};

function buildUrl(url, options) {
	if (options === undefined || options === null || options.params === undefined || options.params === null || options.params.length === 0) {
		return url;
	}
	for (var i = 0; i < options.params.length; i ++) {
		if (i === 0) {
			url += '?' + options.params[i].name + '=' + options.params[i].value;
		} else {
			url += '&' + options.params[i].name + '=' + options.params[i].value;
		}
	}
	return url;
}


